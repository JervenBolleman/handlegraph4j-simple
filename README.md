# handlegraph4j simple

An very simple implementation of the interface's in java following the [libhandlegraph](https://github.com/vgteam/libhandlegraph) code.

That library defines a set of interfaces for "Handle Graphs": genome variation graphs where all access to the graphs is mediated by opaque "handle" objects.

## implementation

This is a very efficient in memory or memory mapped implementation of a handlegraph. 
It can serve 21GB GFA files from under 12GB of RAM, or 6.5 GB byte buffered data structures.
It is used by [sapfhir-cli](https://github.com/JervenBolleman/sapfhir-cli) to demonstrate that 
SPARQL queries can be answered quickly over large variation graphs.

# Usage Instructions

Build as usual with maven

```
mvn install
```

# Copyright/License

See LICENSE.md.

