package vegas.sony.protocol;

import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.ZeusFrameDecoder;
import vegas.sony.protocol.ZeusFrameEncoder;
import vegas.sony.protocol.ZeusPacketDecoder;
import vegas.sony.protocol.ZeusPacketDirection;
import vegas.sony.protocol.ZeusPacketEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class ZeusClientChannelInitializer
extends ChannelInitializer<SocketChannel> {
    final ZeusConnectionManager d;

    public ZeusClientChannelInitializer(ZeusConnectionManager zeusConnectionManager) {
        this.d = zeusConnectionManager;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        this.initSocketChannel(socketChannel);
    }

    protected void initSocketChannel(SocketChannel socketChannel) {
        ChannelPipeline channelPipeline = socketChannel.pipeline();
        channelPipeline.addLast(new ChannelHandler[]{new ReadTimeoutHandler(30)}).addLast(new ChannelHandler[]{new ZeusFrameDecoder()}).addLast(new ChannelHandler[]{new ZeusPacketDecoder(ZeusPacketDirection.CLIENT, true)}).addLast(new ChannelHandler[]{new ZeusFrameEncoder()}).addLast(new ChannelHandler[]{new ZeusPacketEncoder(ZeusPacketDirection.SERVER, true)}).addLast(new ChannelHandler[]{ZeusConnectionManager.m(this.d)});
    }
}
