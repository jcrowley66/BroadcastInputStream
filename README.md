# BroadcastInputStream
Java Inputstream which may be broadcast to 1-N consumers in parallel.

Example - read an XML inputstream from another process, and both parse the XML and write the data to a file. Two BroadcastConsumers may operate in parallel in separate threads to handle these requirements.

Uses shared internal buffers to minimize memory and also reduce synchronization operations.
