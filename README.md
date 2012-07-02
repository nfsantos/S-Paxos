S-Paxos 1.0
==========

Description
-----------
S-Paxos is a Java implementation of Paxos with focus on high-throughput and scalability. 
S-Paxos is built on top of JPaxos (https://github.com/nfsantos/JPaxos), an efficient
multi-threaded implementation of Paxos, extending it with techniques that distribute
the load that is usually concentrated at the leader across all replicas. This allows
all replicas to contribute equally to the work of receiving and disseminating client
requests, thereby greatly alleviating the bottleneck at the leader. As a result, 
S-Paxos achieves in general much higher throughputs than a leader-centric implementation
of Paxos, and is also able to increase its performance with the number of replicas
(up to 7-11 replicas).

You are free to use S-Paxos as an experimental platform for research into
software-based replication or as a library for your commercial products,
provided that the LGPL3.0 licence is respected (see the LICENCE file).
Any publication describing work that uses S-Paxos in some way, should cite
the paper mentioned below.


Publications
------------
Martin Biely, Zarko Milosevic, Nuno Santos, André Schiper
S-Paxos: Offloading the Leader for High Throughput State Machine Replication.
In 31st IEEE International Symposium on Reliable Distributed Systems (SRDS 2012),
Irvine, California, USA, October 8-11, 2012.

License
-------
This software is distributed under the LGPL licence.


Contact and authors
-------------------
S-Paxos was developed at the Distributed System Laboratory (LSR-EPFL), based
on previous work done in collaboration with the Poznan University of Technology (PUT).

Contributors:
* Nuno Santos
* Zarko Milosevic
* Martin Biely
* Andre Schiper

