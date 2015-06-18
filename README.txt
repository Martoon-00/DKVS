How to use:

Launch com.company.Main

-) To launch a node, type in console "dkvs_node <id>"

-) To launch an automatic client, type "client_auto <id> [<delay>]", it will send random requests with specified delay (default 3000 ms.)

-) To launch a manual client, type "client_console <id>".

Id for clients influences only on output logs and Judge functionality. When several clients with same id are launched under the same runtime, Judge may display errors which actually have no place
(so in release version it may be preferred to disable judge and launch all clients with zero id)



When nodes and client are launched under same runtime and Judge.ACTIVE is set to 'true', following errors will be logged:
-) different leaders exist at same term
-) different nodes committed different logs at same index
and warnings:
-) client waits for operation for too much time
These features become disabled depending on how nodes and clients are broken up among runtimes

Also when judge is active, nodes on receiving a message will rarely fall asleep (that message will be lost)


Notes:

-) If logs should be redirected to file, make sure ru.ifmo.ivanov.lang.misc.LogText.COLORED is set to false.

-) To communicate with nodes, you may extend ru.ifmo.ivanov.lang.client.RaftClient and use sendNewOperation() method.
