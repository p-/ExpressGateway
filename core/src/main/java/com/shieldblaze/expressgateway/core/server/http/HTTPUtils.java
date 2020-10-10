/*
 * This file is part of ShieldBlaze ExpressGateway. [www.shieldblaze.com]
 * Copyright (c) 2020 ShieldBlaze
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
package com.shieldblaze.expressgateway.core.server.http;

import com.shieldblaze.expressgateway.core.configuration.http.HTTPConfiguration;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2HeadersEncoder;
import io.netty.handler.codec.http2.Http2PromisedRequestVerifier;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpObjectAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpObjectAdapterBuilder;

final class HTTPUtils {

    static void setGenericHeaders(HttpHeaders headers) {
        headers.set(HttpHeaderNames.SERVER, "ShieldBlaze ExpressGateway");
    }

    static HttpToHttp2ConnectionHandler h2Handler(HTTPConfiguration httpConfiguration, boolean forServer) {
        Http2Settings http2Settings = Http2Settings.defaultSettings();
//        http2Settings.initialWindowSize(httpConfiguration.getInitialWindowSize());
//        http2Settings.maxConcurrentStreams(httpConfiguration.getMaxConcurrentStreams());
//        http2Settings.maxHeaderListSize(httpConfiguration.getMaxHeaderSizeList());
//        http2Settings.headerTableSize(httpConfiguration.getMaxHeaderTableSize());
//        http2Settings.pushEnabled(httpConfiguration.enableHTTP2Push());

        Http2Connection connection = new DefaultHttp2Connection(forServer);

        InboundHttp2ToHttpObjectAdapter listener = new InboundHttp2ToHttpObjectAdapterBuilder(connection)
                .propagateSettings(false)
                .validateHttpHeaders(true)
                .maxContentLength(httpConfiguration.getMaxContentLength())
                .build();

        Http2FrameReader reader = new DefaultHttp2FrameReader(new DefaultHttp2HeadersDecoder(true, http2Settings.maxHeaderListSize()));
        Http2FrameWriter writer = new DefaultHttp2FrameWriter(Http2HeadersEncoder.NEVER_SENSITIVE, false);

        Http2ConnectionEncoder encoder = new HTTP2ContentCompressor(new DefaultHttp2ConnectionEncoder(connection, writer), httpConfiguration);

        DefaultHttp2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, reader,
                Http2PromisedRequestVerifier.ALWAYS_VERIFY, true, true);

        return new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(new HTTP2ContentDecompressor(connection, listener))
                .codec(decoder, encoder)
                .initialSettings(http2Settings)
                .build();
    }

    /**
     * Create new {@link HttpServerCodec} Instance
     * @param httpConfiguration {@link HTTPConfiguration} Instance
     */
    static HttpServerCodec newServerCodec(HTTPConfiguration httpConfiguration) {
        int maxChunkSize = httpConfiguration.getMaxChunkSize();
        return new HttpServerCodec(httpConfiguration.getMaxInitialLineLength(), httpConfiguration.getMaxHeaderSize(), maxChunkSize, true);
    }

    /**
     * Create new {@link HttpClientCodec} Instance
     * @param httpConfiguration {@link HTTPConfiguration} Instance
     */
    static HttpClientCodec newClientCodec(HTTPConfiguration httpConfiguration) {
        int maxInitialLineLength = httpConfiguration.getMaxInitialLineLength();
        int maxHeaderSize = httpConfiguration.getMaxHeaderSize();
        int maxChunkSize = httpConfiguration.getMaxChunkSize();
        return new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, true);
    }
}