package edlf

/**
 * Logic is our logic schema. Provide methods to create logic componets and
 * inputs nodes where to post new changes. Every components can define one or more
 * outputs points.
 * All inputs and outputs point have
 *
 * - unique name
 * - finite state value
 * - state description
 *
 * Is it possible to register a queue to get all output change notifications.
 *
 * Every logic surround a DAG graph and cannot contains cycles. Every nodes can be configured
 * after build it. The logic cannot change after first event (it is not dynamic).
 */

class Logic(){

}
