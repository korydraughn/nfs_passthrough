# nfs_passthrough

See [DESIGN.md](DESIGN.md) for details regarding the design of the system.

## Build
```bash
mvn clean install -DskipTests
```

## Run
The following line will launch the NFS server on port 2049.
```bash
java -jar target/nfs-passthrough-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
          /path/to/exports/file \
          /path/to/root/directory
```

`/path/to/exports/file` is the path to a file containing export entries. This file should be writable by the server. On an export file update request, the server will do the following:
1. Create a temporary file with the new export file content.
2. Backup the current export file using the following filename scheme /path/to/exports/file.backup_\<time_since_epoch\>.
3. Valid export entries will cause the current export file to be replaced by the temporary file.

Use `-h` to view help text.
Use `-p` to launch the server on a port other than 2049.

## Updating the exports file
Two scripts have been provided for demo/test purposes.
- _update_exports.py_: Updates the exports file.
    - To change the export table used by the server, edit the `exports` variable in the script.
- _get_export_file_contents.py_: Fetches and prints the active export table used by the server.

## TODOs
- [working impl] Implement socket-based API for manual refreshing of ID mapper
- [working impl] Implement passthrough filesystem functions
- Implement testing of protocol
