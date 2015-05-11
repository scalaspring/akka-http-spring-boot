
#### Akka Streams Notes

* Remember it's *pull* based, not push based. Walk through flows from the end to the beginning, not the other way around.
* Get to know the operators
** Example: prefixAndTail - is it really a tuple? nope, it's a stream of tuples
* Use logging to help
** Provide practical example of using it to debug

* Examples
** Broadcast with subsequent zip
*** Streams must be same size - add buffers on non-drop side to equalize
** Broadcast with concat - NO!