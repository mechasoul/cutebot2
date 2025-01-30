package my.cute.udpqueue;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import net.dv8tion.jda.api.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.api.audio.factory.IPacketProvider;

/*
 * notes on this class / the entire udpqueue problem as a whole:
 * fair warning: i have zero experience with / knowledge of sockets and networking shit, and all of this is the result of a few
 * days' worth of researching and testing stuff
 * 
 * using udpqueue disables audio receive functionality because (to my understanding) sending audio data to discord will cause
 * discord to send its audio data to the address that's sending it data, unavoidably. udpqueue binds a new socket that's different
 * from the socket used by JDA, so received audio ends up diverted to udpqueue's socket, which has no receive functionality. 
 * 
 * there are a couple ways to fix this that i can think of: 1) have udpqueue send its data with JDA's udp socket, rather than binding
 * its own (this is the approach i use). 2) have udpqueue receive on its socket (this would probably need to happen in a separate 
 * thread, with the socket shared between send/receive processes?), then either udpqueue notifies JDA that it has an audio packet
 * ready for processing, or JDA loops and checks udpqueue's receive queue (essentially replacing JDA's current DatagramSocket.receive()
 * call with some native call to check udpqueue for received data)
 * 
 * both of these approaches have problems. 1) we need to pass JDA's udp socket file descriptor to udpqueue, and java does not want us
 * to access the file descriptor (abstraction reasons probably? i know socket stuff is platform-dependent). consequently i'm 
 * currently using reflection to get the file descriptor, and it's really brittle and sucks a lot and i don't like it. i wonder 
 * if there's a way to retrieve the same socket in udpqueue, like if there's some functionality to try to create or bind a socket 
 * with a specific ip/port and have it return an existing socket fd that matches the provided params? probably not since the java 
 * process owns the socket or something? idk 2) besides the fact that this is considerably more involved due to requiring additional
 * native code (tbf it's also a much more permanent/correct approach), it also requires some JDA internals to be changed, as far as i
 * can tell. there's not currently any functionality for receive system implementation like with send system (ie, there's no 
 * IAudioReceiveSystem), so i don't think we can actually eg change jda's receive loop to ask udpqueue for data rather than call
 * receive on its udp socket. well tbf we maybe COULD but it would require using DirectAudioController / VoiceDispatchInterceptor
 * to manage audio entirely myself which practically speaking would mean copying all of JDA's audio internals, just to make this 
 * one tiny change. it would prooobably be possible but that's a lot of work for what is really just gag functionality. i guess the
 * other approach of having udpqueue call some java method with its audio packet might work? frankly i don't know shit about native/
 * java interaction but that's probably possible, and we could set up some custom listener that udpqueue notifies, which then takes
 * the packet and does the same processing as jda does to its packets, then passes the processed data to AudioReceiveHandler. that
 * might be feasible, maybe look into that
 * 
 * oh it's also worth noting that just passing JDA's udp socket to udpqueue was causing error 10014 from winsock when it tried to 
 * send, which was a consequence of JDA's udp socket being bound to an ipv6 address, i think? or using ipv6 addressing or something
 * about ipv6, i dont know. anyway udpqueue wants ipv4 so this caused wsaefault. fixed by setting -Djava.net.preferIPv4Stack=true 
 * for jvm.
 * 
 * TODO if i ever want to make this code better
 * fix the dumb loading call in UdpQueueManagerLibrary
 * look into native receive as listed above: udpqueue sets up its own receive loop (probably demands another thread), shares socket
 * between send/receive threads. on receive, calls some java method to notify a listener and provide the audio packet, which then
 * gets the same processing as in jda's AudioConnection before being passed to our AudioReceiveListener (i think jda's resources 
 * used in processing are public?)
 */
/**
 * very slightly tweaked version of sedmelluq's (+MinnDevelopment's?) jda-nas/udpqueue to enable audio receive with udpqueue
 */
public class NativeAudioSendSystem implements IAudioSendSystem {
    private final long queueKey;
    private final NativeAudioSendFactory audioSendSystem;
    private final IPacketProvider packetProvider;
    private int fd = -1;

    public NativeAudioSendSystem(long queueKey, NativeAudioSendFactory audioSendSystem, IPacketProvider packetProvider) {
        this.queueKey = queueKey;
        this.audioSendSystem = audioSendSystem;
        this.packetProvider = packetProvider;
    }

    @Override
    public void start() {
    	fd = SocketUtils.getFd(packetProvider.getUdpSocket());
        audioSendSystem.addInstance(this);
    }

    @Override
    public void shutdown() {
        audioSendSystem.removeInstance(this);
    }

    void populateQueue(UdpQueueManager queueManager) {
        int remaining = queueManager.getRemainingCapacity(queueKey);
        boolean emptyQueue = queueManager.getCapacity() - remaining > 0;

        for (int i = 0; i < remaining; i++) {
            ByteBuffer packet = packetProvider.getNextPacketRaw(emptyQueue);
            InetSocketAddress address = packetProvider.getSocketAddress();

            if (packet == null || !queueManager.queuePacketWithSocket(queueKey, packet, address, fd)) {
                break;
            }
        }
    }
}