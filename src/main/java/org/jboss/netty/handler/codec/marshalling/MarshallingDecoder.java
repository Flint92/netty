/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.handler.codec.marshalling;

import java.io.StreamCorruptedException;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
/**
 * Decoder which MUST be used with {@link MarshallingEncoder}.
 * 
 * A {@link LengthFieldBasedFrameDecoder} which use an {@link Unmarshaller} to read the Object out of the {@link ChannelBuffer}.
 *
 */
public class MarshallingDecoder extends LengthFieldBasedFrameDecoder {

    private final UnmarshallerProvider provider;

    /**
     * Creates a new decoder whose maximum object size is {@code 1048576}
     * bytes.  If the size of the received object is greater than
     * {@code 1048576} bytes, a {@link StreamCorruptedException} will be
     * raised.
     * 
     */
    public MarshallingDecoder(UnmarshallerProvider provider) {
        this(provider, 1048576);
    }
    
    /**
     * Creates a new decoder with the specified maximum object size.
     *
     * @param maxObjectSize  the maximum byte length of the serialized object.
     *                       if the length of the received object is greater
     *                       than this value, {@link TooLongFrameException}
     *                       will be raised.
     */
    public MarshallingDecoder(UnmarshallerProvider provider, int maxObjectSize) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.provider = provider;
    }
    
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

        ChannelBuffer frame = (ChannelBuffer) super.decode(ctx, channel, buffer);
        if (frame == null) {
            return null;
        }

        Unmarshaller unmarshaller = provider.getUnmarshaller(channel);
        ByteInput input = new ChannelBufferByteInput(buffer);
        
        try {
            unmarshaller.start(input);
            Object obj = unmarshaller.readObject();
            unmarshaller.finish();
            return obj;
        } finally {
            // Call close in a finally block as the ReplayingDecoder will throw an Error if not enough bytes are
            // readable. This helps to be sure that we do not leak resource
            unmarshaller.close();
        }
    }

    @Override
    protected ChannelBuffer extractFrame(ChannelBuffer buffer, int index, int length) {
        return buffer.slice(index, length);
    }
}
