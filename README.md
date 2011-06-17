Amanzi Index
============

This is a library of utilities for creating and using multi-property indexes
in Neo4j graph databases.

Status
------

The current status is that this is still under development. At the moment
there exists:
* A suite of general purpose mappers for converting properties into integers
for inclusion in the combined index tree. The purpose of this is to get all
properties onto the same level, integers, and thereby be able to build a
single tree that contains all properties. The mappers also ensure sort order
is maintained, allowing for conditional queries on the index (eg. range queries).
* Construction of a n-dimensional index tree for all named properties of a
set of nodes. Included is support for nodes that are missing the properties.
These are assigned a special index value, and will be returned for conditions
not mentioning that property, and not returned if the query does mention that
property.
* Initial search support including a text query language supporting AND, OR
>=, >, <= and < conditions. For example: propA >= 5 and propA < 20 or propB == AA.
The query language is converted to an internal nested condition model, which
is also available for direct programmatic construction of the queries.

The way the search query is performed is by:
* Converting the query conditions into level-specific conditions that match
the internal integer keys of each level, optimizing the tests at each level.
* Storing both the level specific tests and the end-node tests (the original
query structures) in the same objects which are applied to both the index tree
nodes and the data nodes in a traverser.
* The traverser is wrapped in a special result structure which can be iterated
over to stream the results out of the database without loading memory. 

The ultimate goal is an index that actually performs faster the more complex
(and more restrictive) the database conditions are. A query that limits on
many properties at once will not result in several result-sets followed by
a set intersection, instead all conditions are evaluated at once (at each
node), trimming the result set on the fly, and reducing the total number of
tests performed, and therefor also improving the database query performance.

Update: 17th June
-----------------

We developed the initial search capability, including a basic text query
language. This has been tested on a few very simple cases. See the unit
tests for examples.

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

