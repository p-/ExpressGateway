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

import com.shieldblaze.expressgateway.configuration.http.HttpConfiguration;
import com.shieldblaze.expressgateway.protocol.http.compression.HTTP2ContentCompressor;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2HeadersEncoder;
import io.netty.handler.codec.http2.Http2PromisedRequestVerifier;
import io.netty.handler.codec.http2.Http2Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;

public final class HTTPCodecs {

    private static final Logger logger = LogManager.getLogger(HTTPCodecs.class);

    public static Http2FrameCodec http2ClientCodec(HttpConfiguration httpConfiguration) {
        Http2Settings http2Settings = new Http2Settings();
        http2Settings.initialWindowSize(httpConfiguration.h2InitialWindowSize());
        http2Settings.maxConcurrentStreams(httpConfiguration.h2MaxConcurrentStreams());
        http2Settings.maxHeaderListSize(httpConfiguration.h2MaxHeaderListSize());
        http2Settings.headerTableSize(httpConfiguration.h2MaxHeaderTableSize());
        http2Settings.maxFrameSize(httpConfiguration.h2MaxFrameSize());

        Http2Connection connection = new DefaultHttp2Connection(false);

        Http2FrameReader reader = new DefaultHttp2FrameReader(new DefaultHttp2HeadersDecoder(true, http2Settings.maxHeaderListSize()));
        Http2FrameWriter writer = new DefaultHttp2FrameWriter(Http2HeadersEncoder.NEVER_SENSITIVE, false);

        Http2ConnectionEncoder encoder = new HTTP2ContentCompressor(new DefaultHttp2ConnectionEncoder(connection, writer), httpConfiguration);

        DefaultHttp2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, reader,
                Http2PromisedRequestVerifier.ALWAYS_VERIFY, true, true);

        try {
            Constructor<Http2FrameCodec> constructor = Http2FrameCodec.class.getDeclaredConstructor(
                    Http2ConnectionEncoder.class,
                    Http2ConnectionDecoder.class,
                    Http2Settings.class,
                    boolean.class,
                    boolean.class);
            constructor.setAccessible(true);
            Object[] obj = {encoder, decoder, http2Settings, false, true};
            Http2FrameCodec http2FrameCodec = constructor.newInstance(obj);
            decoder.frameListener(new DelegatingDecompressorFrameListener(connection, decoder.frameListener()));

            return http2FrameCodec;
        } catch (Exception ex) {
            logger.fatal("Failed to initialize Http2FrameCodec", ex);
            return null;
        }
    }

    public static Http2FrameCodec http2ServerCodec(HttpConfiguration httpConfiguration) {
        Http2Settings http2Settings = new Http2Settings();
        http2Settings.maxHeaderListSize(httpConfiguration.h2MaxHeaderListSize());

        Http2Connection connection = new DefaultHttp2Connection(true);

        Http2FrameReader reader = new DefaultHttp2FrameReader(new DefaultHttp2HeadersDecoder(true, http2Settings.maxHeaderListSize()));
        Http2FrameWriter writer = new DefaultHttp2FrameWriter(Http2HeadersEncoder.NEVER_SENSITIVE, false);

        Http2ConnectionEncoder encoder = new HTTP2ContentCompressor(new DefaultHttp2ConnectionEncoder(connection, writer), httpConfiguration);

        DefaultHttp2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, reader,
                Http2PromisedRequestVerifier.ALWAYS_VERIFY, true, true);

        try {
            Constructor<Http2FrameCodec> constructor = Http2FrameCodec.class.getDeclaredConstructor(
                    Http2ConnectionEncoder.class,
                    Http2ConnectionDecoder.class,
                    Http2Settings.class,
                    boolean.class,
                    boolean.class);
            constructor.setAccessible(true);
            Object[] obj = {encoder, decoder, http2Settings, false, true};
            Http2FrameCodec http2FrameCodec = constructor.newInstance(obj);
            decoder.frameListener(new DelegatingDecompressorFrameListener(connection, decoder.frameListener()));

            return http2FrameCodec;
        } catch (Exception ex) {
            logger.fatal("Failed to initialize Http2FrameCodec", ex);
            return null;
        }
    }

    /**
     * Create new {@link HttpServerCodec} Instance
     *
     * @param httpConfiguration {@link HttpConfiguration} Instance
     */
    static HttpServerCodec http1ServerCodec(HttpConfiguration httpConfiguration) {
        int maxInitialLineLength = httpConfiguration.maxInitialLineLength();
        int maxHeaderSize = httpConfiguration.maxHeaderSize();
        int maxChunkSize = httpConfiguration.maxChunkSize();
        return new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, true);
    }

    /**
     * Create new {@link HttpClientCodec} Instance
     *
     * @param httpConfiguration {@link HttpConfiguration} Instance
     */
    public static HttpClientCodec http1ClientCodec(HttpConfiguration httpConfiguration) {
        int maxInitialLineLength = httpConfiguration.maxInitialLineLength();
        int maxHeaderSize = httpConfiguration.maxHeaderSize();
        int maxChunkSize = httpConfiguration.maxChunkSize();
        return new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, false);
    }
}
