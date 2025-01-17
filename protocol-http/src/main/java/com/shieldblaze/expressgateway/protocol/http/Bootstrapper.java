/*
 * This file is part of ShieldBlaze ExpressGateway. [www.shieldblaze.com]
 * Copyright (c) 2020-2022 ShieldBlaze
 *
 * ShieldBlaze ExpressGateway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ShieldBlaze ExpressGateway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ShieldBlaze ExpressGateway.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.shieldblaze.expressgateway.protocol.http;

import com.shieldblaze.expressgateway.backend.Node;
import com.shieldblaze.expressgateway.backend.NodeBytesTracker;
import com.shieldblaze.expressgateway.core.factory.BootstrapFactory;
import com.shieldblaze.expressgateway.core.handlers.ConnectionTimeoutHandler;
import com.shieldblaze.expressgateway.protocol.http.adapter.http1.HTTPOutboundAdapter;
import com.shieldblaze.expressgateway.protocol.http.adapter.http2.HTTP2OutboundAdapter;
import com.shieldblaze.expressgateway.protocol.http.alpn.ALPNHandler;
import com.shieldblaze.expressgateway.protocol.http.alpn.ALPNHandlerBuilder;
import com.shieldblaze.expressgateway.protocol.http.loadbalancer.HTTPLoadBalancer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslHandler;

import java.time.Duration;

final class Bootstrapper {

    private final HTTPLoadBalancer httpLoadBalancer;
    private final EventLoopGroup eventLoopGroup;
    private final ByteBufAllocator byteBufAllocator;

    Bootstrapper(HTTPLoadBalancer httpLoadBalancer) {
        this.httpLoadBalancer = httpLoadBalancer;
        this.eventLoopGroup = httpLoadBalancer.eventLoopFactory().childGroup();
        this.byteBufAllocator = httpLoadBalancer.byteBufAllocator();
    }

    HTTPConnection newInit(Node node, Channel channel) {
        HTTPConnection httpConnection = new HTTPConnection(node);

        Bootstrap bootstrap = BootstrapFactory.tcp(httpLoadBalancer.configurationContext(), eventLoopGroup, byteBufAllocator);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addFirst(new NodeBytesTracker(node));

                Duration timeout = Duration.ofMillis(httpLoadBalancer.configurationContext().transportConfiguration().connectionIdleTimeout());
                pipeline.addLast(new ConnectionTimeoutHandler(timeout, false));

                DownstreamHandler downstreamHandler = new DownstreamHandler(channel);
                httpConnection.downstreamHandler(downstreamHandler);

                if (!httpLoadBalancer.configurationContext().tlsClientConfiguration().enabled()) {
                    pipeline.addLast(HTTPCodecs.http1ClientCodec(httpLoadBalancer.httpConfiguration()));
                    pipeline.addLast(new HttpContentDecompressor());
                    pipeline.addLast(new HTTPOutboundAdapter());
                    pipeline.addLast(downstreamHandler);
                } else {
                    String hostname = node.socketAddress().getHostName();
                    int port = node.socketAddress().getPort();
                    SslHandler sslHandler = httpLoadBalancer.configurationContext().tlsClientConfiguration()
                            .defaultMapping()
                            .sslContext()
                            .newHandler(ch.alloc(), hostname, port);

                    ALPNHandler alpnHandler = ALPNHandlerBuilder.newBuilder()
                            // HTTP/2 Handlers
                            .withHTTP2ChannelHandler(HTTPCodecs.http2ClientCodec(httpLoadBalancer.httpConfiguration()))
                            .withHTTP2ChannelHandler(new HTTP2OutboundAdapter())
                            .withHTTP2ChannelHandler(downstreamHandler)
                            // HTTP/1.1 Handlers
                            .withHTTP1ChannelHandler(HTTPCodecs.http1ClientCodec(httpLoadBalancer.httpConfiguration()))
                            .withHTTP1ChannelHandler(new HttpContentDecompressor())
                            .withHTTP1ChannelHandler(new HTTPOutboundAdapter())
                            .withHTTP1ChannelHandler(downstreamHandler)
                            .build();

                    pipeline.addLast(sslHandler);
                    pipeline.addLast(alpnHandler);
                }
            }
        });

        ChannelFuture channelFuture = bootstrap.connect(node.socketAddress());
        httpConnection.init(channelFuture);
        return httpConnection;
    }
}
