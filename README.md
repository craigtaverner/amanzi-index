Amanzi Index
============

This is a library of utilities for creating and using multi-property indexes
in Neo4j graph databases.

Status
------

The current status is that this is very much under development. There is no
actual index yet. We are building a suite of general purpose mappers for
converting properties into integers for inclusion in the combined index
tree. The purpose of this is to get all properties onto the same level,
integers, and thereby be able to build a single tree that contains all
properties. The mappers also ensure sort order is maintained, allowing for
conditional queries on the index (eg. range queries).

Once the mappers are complete, we will build the index model itself, which
will analyse the incoming data and make choices for the configuration of the
mappers, and from that build the tree. Then we will write the query parser
which can analyse the database query, and express it in terms of the tree
structure, traversing the tree performing only the necessary tests at each
node to arrive at the final data set.

The ultimate goal is an index that actually performs faster the more complex
(and more restrictive) the database conditions are. A query that limits on
many properties at once will not result in several results-sets followed by
a set intersection, instead all conditions are evaluated at once (at each
node), trimming the result set on the fly, and reducing the total number of
tests performed, and therefor also improving the database query performance.

Update: 19th Dec
----------------

We added an initial implementation of an index tree with some test code to
check that the tree actually gets built for a number of simple cases,
including at least one multi-property, multi-type case. However the there is
no support for querying the index yet, no support for aggregations on the
index and the test code is not complete enough to be sure the index tree is
completely correct.

This is made available on github only for people interested in following the development and commenting on the design.

Trying it out
-------------

This is a maven project, so the easiest way to try it out is:

  git clone git://github.com/craigtaverner/amanzi-index.git
  cd amanzi-index
  mvn clean test

