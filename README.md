# nfs_passthrough

See [DESIGN.md](DESIGN.md) for details regarding the design of the system.

## Build
```bash
mvn clean install -DskipTests
```

## Run
The following line will launch the NFS server on port 2049.
```bash
java -jar target/nfs-passthrough-0.0.1-SNAPSHOT-jar-with-dependencies.jar /path/to/exports/file
```

Use `-h` to view help text.
Use `-p` to launch the server on a port other than 2049.

## TODOs
- Implement socket-based API for manual refreshing of ID mapper
- Implement passthrough filesystem functions
- Implement testing of protocol
