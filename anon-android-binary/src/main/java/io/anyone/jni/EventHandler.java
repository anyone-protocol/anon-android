// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package io.anyone.jni;

/**
 * Abstract interface whose methods are invoked when Anon sends us an event.
 *
 * @see AnonControlConnection#setEventHandler
 * @see AnonControlConnection#setEvents
 */
public interface EventHandler {

    /**
     * Invoked when a circuit's status has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link AnonControlCommands#CIRC_EVENT_LAUNCHED} :  circuit ID assigned to new circuit</li>
     * <li>{@link AnonControlCommands#CIRC_EVENT_BUILT}    :  all hops finished, can now accept streams</li>
     * <li>{@link AnonControlCommands#CIRC_EVENT_EXTENDED} :  one more hop has been completed</li>
     * <li>{@link AnonControlCommands#CIRC_EVENT_FAILED}   :  circuit closed (was not built)</li>
     * <li>{@link AnonControlCommands#CIRC_EVENT_CLOSED}   :  circuit closed (was built)</li>
     * </ul>
     *
     * <b>circID</b> is the alphanumeric identifier of the affected circuit,
     * and <b>path</b> is a comma-separated list of alphanumeric ServerIDs.
     */
    void circuitStatus(String status, String circID, String path);

    /**
     * Invoked when a stream's status has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link AnonControlCommands#STREAM_EVENT_SENT_CONNECT}: Sent a connect cell along a circuit</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_SENT_RESOLVE}: Sent a resolve cell along a circuit</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_SUCCEEDED}: Received a reply; stream established</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_FAILED}: Stream failed and not retriable</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_CLOSED}: Stream closed</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_NEW}: New request to connect</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_NEW_RESOLVE}: New request to resolve an address</li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_FAILED_RETRIABLE}: </li>
     * <li>{@link AnonControlCommands#STREAM_EVENT_REMAP}: </li>
     * </ul>
     *
     * <b>streamID</b> is the alphanumeric identifier of the affected stream,
     * and its <b>target</b> is specified as address:port.
     */
    void streamStatus(String status, String streamID, String target);

    /**
     * Invoked when the status of a connection to an OR has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link AnonControlCommands#OR_CONN_EVENT_LAUNCHED}</li>
     * <li>{@link AnonControlCommands#OR_CONN_EVENT_CONNECTED}</li>
     * <li>{@link AnonControlCommands#OR_CONN_EVENT_FAILED}</li>
     * <li>{@link AnonControlCommands#OR_CONN_EVENT_CLOSED}</li>
     * <li>{@link AnonControlCommands#OR_CONN_EVENT_NEW}</li>
     * </ul>
     * <b>orName</b> is the alphanumeric identifier of the OR affected.
     */
    void orConnStatus(String status, String orName);

    /**
     * Invoked once per second. <b>read</b> and <b>written</b> are
     * the number of bytes read and written, respectively, in
     * the last second.
     */
    void bandwidthUsed(long read, long written);

    /**
     * Invoked whenever Anon learns about new ORs.  The <b>orList</b> object
     * contains the alphanumeric ServerIDs associated with the new ORs.
     */
    void newDescriptors(java.util.List<String> orList);

    /**
     * Invoked when Anon logs a message.
     * <b>severity</b> is one of:
     * <ul>
     * <li>{@link AnonControlCommands#EVENT_DEBUG_MSG}</li>
     * <li>{@link AnonControlCommands#EVENT_INFO_MSG}</li>
     * <li>{@link AnonControlCommands#EVENT_NOTICE_MSG}</li>
     * <li>{@link AnonControlCommands#EVENT_WARN_MSG}</li>
     * <li>{@link AnonControlCommands#EVENT_ERR_MSG}</li>
     * </ul>
     * and <b>msg</b> is the message string.
     */
    void message(String severity, String msg);

    /**
     * Invoked when an unspecified message is received.
     *
     * @param type the message type
     * @param msg  the message string
     */
    void unrecognized(String type, String msg);
}
