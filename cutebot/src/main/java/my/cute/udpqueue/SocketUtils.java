package my.cute.udpqueue;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.nio.ch.DatagramSocketAdaptor;

public class SocketUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(SocketUtils.class);
	
	/**
	 * given a FileDescriptor, attempt to extract the descriptor value via reflection. assumes it exists as an int in a field
	 * called "fd"
	 * @param fd
	 * @return the file descriptor value, or -1 if the value couldn't be obtained
	 */
	static int getFd(FileDescriptor fd) {
		try {
			Field fdField = fd.getClass().getDeclaredField("fd");
			fdField.setAccessible(true);
			return fdField.getInt(fd);
		} catch (Exception e) {
			logger.info("couldn't get fd from FileDescriptor?", e);
			return -1;
		}
	}
	
	/**
	 * extract the file descriptor from the provided socket via reflection. java doesn't want us to know about the fd for
	 * abstraction reasons i guess, but we need it so we do this. this is super brittle and kind of insane code but the
	 * ends justify the means????
	 * @param socket the socket to extract the fd from. can be a DatagramSocket, NetMulticastSocket, or DatagramSocketAdaptor
	 * @return the fd for the provided socket if one was successfully extracted, or -1 otherwise
	 */
	static int getFd(DatagramSocket socket) {
		/*
		 * broadly, socket should be one of 3 types: DatagramSocket, java.net.NetMulticastSocket, or 
		 * sun.nio.ch.DatagramSocketAdaptor (there are other DatagramSocket subclasses eg MulticastSocket, but i'm just 
		 * encountering DatagramSocket, and handling DatagramSocket in my java version requires handling the other two, so...).
		 * if it's a DatagramSocket, then there are two possibilities: older versions have an "impl" field that holds a 
		 * DatagramSocketImpl, which has an "fd" field with a FileDescriptor. newer versions (since when?) have a "delegate"
		 * field that holds a DatagramSocketAdaptor or a NetMulticastSocket.
		 * if it's a DatagramSocketAdaptor, it has a "dc" field with a DatagramChannelImpl, which has a "fdVal" field with the
		 * file descriptor int value.
		 * if it's a NetMulticastSocket, it has an "impl" field with a DatagramSocketImpl, as above.
		 * we perform type checking to handle DatagramSocket and DatagramSocketAdaptor, and failing those, assume it's a 
		 * NetMulticastSocket.
		 * 
		 * this is, of course, insanely brittle code since it's all implementation-dependent. there are maybe other possible 
		 * DatagramSocket subclasses that could be used here, especially on other platforms (i'm sure some of this must be windows-
		 * dependent). i'm not super concerned though because a) this isn't intended to be a portable project; b) if i do move to
		 * hosting on linux i can just...change this if necessary; c) this is all just for dumb gag functionality
		 * 
		 * also, sun.nio.ch is a jdk internal package and isn't normally accessible, so despite DatagramSocketAdaptor being public,
		 * we have to specifically expose the package (heh) to access DatagramSocketAdaptor like this for instanceof purposes. 
		 * java.net.NetMulticastSocket is package-private, so we have to do dynamic type-checking instead. could not type check at 
		 * all and just try to access fields and aggressively catch exceptions but that gives me the willies
		 * 
		 * need add-opens everywhere and whatever
		 */
		if(socket instanceof DatagramSocketAdaptor) {
			return getFdFromDatagramSocketAdaptor(socket);
		} else if(socket instanceof DatagramSocket) {
			return getFdFromDatagramSocket(socket);
		} else {
			return getFdFromNetMulticastSocket(socket);
		}
	}
	
	/**
	 * given a DatagramSocketAdaptor, extracts its file descriptor value via reflection. assumes the provided 
	 * DatagramSocketAdaptor has a field called "dc" (in current version of DatagramSocketAdaptor, this is a DatagramChannelImpl),
	 * which holds an object with a field called "fdVal", which holds the fd int val
	 * @param socket a DatagramSocketAdaptor. note that no type validation is performed in this method
	 * @return the fd value ,or -1 if no fd value could be retrieved
	 */
	private static int getFdFromDatagramSocketAdaptor(Object socket) {
		try {
			Field dcField = socket.getClass().getDeclaredField("dc");
			dcField.setAccessible(true);
			Object dc = dcField.get(socket);
			//dc is DatagramChannelImpl
			Field fdField = dc.getClass().getDeclaredField("fdVal");
			fdField.setAccessible(true);
			return fdField.getInt(dc);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			logger.info("unable to extract fd from DatagramSocketAdaptor", e);
			return -1;
		}
	}
	
	/**
	 * given a DatagramSocket, extracts its file descriptor value via reflection. assumes the DatagramSocket either has a 
	 * field called "impl" that holds a DatagramSocketImpl (as in older java versions), or a field called "delegate" that holds
	 * a DatagramSocketAdaptor or NetMulticastSocket (as in newer java versions)
	 * @param socket
	 * @return the fd value, or -1 if no fd value could be retrieved
	 */
	private static int getFdFromDatagramSocket(DatagramSocket socket) {
		Object delegate = extractDelegate(socket);
		if(delegate != null) {
			//delegate should be either a DatagramSocketAdaptor or a NetMulticastSocket (or there's been an implementation change...)
			if(delegate instanceof DatagramSocketAdaptor) {
				return getFdFromDatagramSocketAdaptor(delegate);
			} else {
				return getFdFromNetMulticastSocket(delegate);
			}
		} else {
			//no delegate. maybe old datagramsocket?
			DatagramSocketImpl socketImpl = extractImpl(socket);
			if(socketImpl != null) {
				return getFdFromImpl(socketImpl);
			} else {
				//unknown DatagramSocket structure, can't proceed
				logger.info("DatagramSocket missing delegate and impl fields?");
				return -1;
			}
		}
	}
	
	/**
	 * given a NetMulticastSocket, extracts its file descriptor value via reflection. assumes the NetMulticastSocket has a field
	 * called "impl" that holds a DatagramSocketImpl
	 * @param socket
	 * @return
	 */
	private static int getFdFromNetMulticastSocket(Object socket) {
		try {
			//does this even work? especially for classes that aren't visible?
			//my socket type is never NetMulticastSocket so this is untested lol
			Class<?> netMulticastClass = Class.forName("java.net.NetMulticastSocket");
			if(netMulticastClass.isInstance(socket)) {
				DatagramSocketImpl impl = extractImpl(netMulticastClass.cast(socket));
				if(impl != null) {
					return getFdFromImpl(impl);
				} else {
					logger.info("couldn't extract impl from NetMulticastSocket?");
					return -1;
				}
			} else {
				logger.info("unknown socket class: " + socket.getClass().descriptorString());
				return -1;
			}
		} catch (Exception e) {
			logger.info("unable to extract fd from socket object", e);
			return -1;
		}
	}
	
	/**
	 * accepts a subclass of DatagramSocket and attempts to use reflection to access the field "impl" and extract a 
	 * DatagramSocketImpl from it. if the field impl is missing or if the impl field has a reference to an object that isn't
	 * a DatagramSocketImpl or if anything goes wrong for basically any reason, null is returned
	 * @param socket the object to extract the impl field from
	 * @return a reference to the DatagramSocketImpl stored in the provided object's impl field, or null if something goes 
	 * wrong for basically any reason
	 */
	private static DatagramSocketImpl extractImpl(Object socket) {
		try {
			Field implField = socket.getClass().getDeclaredField("impl");
			implField.setAccessible(true);
			Object impl = implField.get(socket);
			if(impl instanceof DatagramSocketImpl) {
				return (DatagramSocketImpl) impl;
			} else {
				logger.info("socket has impl but not DatagramSocketImpl? impl class: " + impl.getClass().toGenericString());
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * given a DatagramSocket, extracts an Object from a field called "delegate"
	 * @param socket
	 * @return the object stored in the socket's delegate field, or null if the delegate could not be retrieved
	 */
	private static Object extractDelegate(DatagramSocket socket) {
		try {
			Field delegateField = socket.getClass().getDeclaredField("delegate");
			delegateField.setAccessible(true);
			Object delegate = delegateField.get(socket);
			return delegate;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			return null;
		}
	}
	
	/**
	 * given a DatagramSocketImpl, extracts a file descriptor value by extracting a FileDescriptor from its "fd" field and 
	 * passing that to {@link #getFd(FileDescriptor)}
	 * @param impl
	 * @return the file descriptor value for the provided DatagramSocketImpl, or -1 if the fd val could not be extracted
	 */
	private static int getFdFromImpl(DatagramSocketImpl impl) {
		try {
			Field fdField = impl.getClass().getDeclaredField("fd");
			fdField.setAccessible(true);
			return getFd((FileDescriptor)fdField.get(impl));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			logger.info("exception accessing fd field on DatagramSocketImpl?");
			return -1;
		}
	}
}
