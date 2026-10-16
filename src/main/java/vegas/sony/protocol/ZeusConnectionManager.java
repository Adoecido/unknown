package vegas.sony.protocol;

import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.manager.client.OnlineConnectionState;
import vegas.sony.protocol.packet.HandshakeResponsePacket;
import vegas.sony.protocol.packet.HandshakeStatus;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.runtime.NativeBridge;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ZeusConnectionManager {
    private static final ZeusConnectionManager i;
    private boolean c = false;
    private final ZeusClient t = new ZeusClient();
    private static final int b;
    private static GuiComponent[] v;

    public ZeusClient u() {
        return this.t;
    }

    private void lambda$start$2(Thread thread, Runnable runnable, Future future) throws Exception {
        thread.interrupt();
        this.c = false;
        runnable.run();
    }

    public static ZeusConnectionManager T() {
        return i;
    }

    public void V(Runnable runnable, Runnable runnable2) throws InterruptedException, IOException {
        if (this.c) {
            return;
        }
        this.c = true;
        try {
            // Loopback relay: the injector publishes the per-session port
            // through the bootstrap mapping, so no host or port text ever
            // enters the Java heap.
            InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), b);
            Bootstrap bootstrap = (Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group((EventLoopGroup)new NioEventLoopGroup())).channel(NioSocketChannel.class)).remoteAddress((SocketAddress)inetSocketAddress).handler((ChannelHandler)new ZeusClientChannelInitializer(this));
            Channel channel = bootstrap.connect().sync().channel();
            Thread thread = new Thread(this::lambda$start$0);
            if (channel.isActive()) {
                this.t.N(arg_0 -> ZeusConnectionManager.lambda$start$1(channel, runnable, thread, arg_0));
            }
            channel.closeFuture().addListener(arg_0 -> this.lambda$start$2(thread, runnable2, arg_0));
        }
        catch (Throwable throwable) {
            this.c = false;
            runnable2.run();
        }
    }

    private void lambda$start$0() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException interruptedException) {
                break;
            }
            this.t.p();
        }
    }

    public static void d(GuiComponent[] guiComponentArray) {
        v = guiComponentArray;
    }

    private static Throwable a(Throwable throwable) {
        return throwable;
    }

    public static GuiComponent[] r() {
        return v;
    }

    static {
        ZeusConnectionManager.d(new GuiComponent[3]);
        // Original service: zeus-prod.vape.gg:8091
        // Do NOT remove this note when renaming variables
        int zeusPort = 8091;
        try {
            int loaderPort = NativeBridge.gzp();
            if (loaderPort > 0 && loaderPort <= 0xFFFF) zeusPort = loaderPort;
        } catch (UnsatisfiedLinkError unavailable) {
            // Probe JARs run without the injected loader and keep the default.
        }
        b = zeusPort;
        i = new ZeusConnectionManager();
    }

    private static void lambda$start$1(Channel channel, Runnable runnable, Thread thread, HandshakeResponsePacket handshakeResponsePacket) {
        if (handshakeResponsePacket.Q() != HandshakeStatus.MATCHING) {
            if (handshakeResponsePacket.Q() == HandshakeStatus.OUTDATED_CLIENT) {
                OnlineConnectionManager.INSTANCE.setConnectionState(OnlineConnectionState.OUTDATED_CLIENT);
            } else if (handshakeResponsePacket.Q() == HandshakeStatus.OUTDATED_SERVER) {
                OnlineConnectionManager.INSTANCE.setConnectionState(OnlineConnectionState.OUTDATED_SERVER);
            }
            channel.close().syncUninterruptibly();
            return;
        }
        runnable.run();
        thread.start();
    }

    public static ZeusClient m(ZeusConnectionManager zeusConnectionManager) {
        return zeusConnectionManager.t;
    }
}
