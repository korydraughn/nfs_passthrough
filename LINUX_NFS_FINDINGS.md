# Linux NFS Server and Realtime Updates

**QUESTION**: Does the NFS server implementation shipped with the Linux kernel support realtime updates to the export table?

## TLDR

Based on documentation and testing, updating the export table of the Linux NFS server is intentionally supported.

## Findings

### man 5 exports

From the **DESCRIPTION** section of the man page:
```
To apply changes to this file, run exportfs -ra or restart the NFS server.
```
_this file_ being `/etc/exports`. This line alone infers that updating the exported entries for the NFS server can be done without restarting the NFS server.

### man 8 exportfs

From the **DESCRIPTION** section of the man page:
```
However, a system administrator can choose to add or delete exports without modifying /etc/exports
or files under /etc/exports.d by using the exportfs command.
```

### exportfs (RedHat documentation)

From [https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/6/html/storage_administration_guide/s1-nfs-server-config-exportfs](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/6/html/storage_administration_guide/s1-nfs-server-config-exportfs):
```
When issued manually, the /usr/sbin/exportfs command allows the root user to selectively export or
unexport directories without restarting the NFS service. When given the proper options, the
/usr/sbin/exportfs command writes the exported file systems to /var/lib/nfs/etab. Since rpc.mountd
refers to the etab file when deciding access privileges to a file system, changes to the list of
exported file systems take effect immediately.
```

## Experimentation and Results

Test system/setup:
- Ubuntu 18.04
- Single exported directory on NFS server
- Two external users, each on different physical machines

Through testing, we've confirmed the following:
- `exportfs -r` updates the export table without requiring a restart of the server
- `exportfs -a` does NOT cause the NFS server to enforce the latest changes to /etc/exports
- Running `exportfs` with the `-v` option is very helpful in determining what is happening
- Duplicate export entries do NOT cause the NFS server to fail
- Duplicate export entries are detected, reported, and ignored
- Names (hostnames, FQDNs, etc) are resolved against DNS and can result in duplicate entry errors being reported and ignored
- The first non-duplicate export entry is kept (subsequent duplicate entries are ignored)

### Test Output (sensitive info replaced with placeholders)

**NOTE:** The terminal's prompt is configured to display non-zero error codes of the previously executed command. The error code is reported using the following format:
```bash
ERROR(non_zero_error_code) $ # <next_command_to_execute>
```

#### Duplicate export entries with different options

```bash
$ cat /etc/exports
# /etc/exports: the access control list for filesystems which may be exported
#               to NFS clients.  See exports(5).
#
# Example for NFSv2 and NFSv3:
# /srv/homes       hostname1(rw,sync,no_subtree_check) hostname2(ro,sync,no_subtree_check)
#
# Example for NFSv4:
# /srv/nfs4        gss/krb5i(rw,sync,fsid=0,crossmnt,no_subtree_check)
# /srv/nfs4/homes  gss/krb5i(rw,sync,no_subtree_check)
#

/some/directory     host2.some.domain.org(secure,rw)
/some/directory     host2.some.domain.org(ro,subtree_check)
/some/directory     host2.some.domain.org(wdelay,ro,no_root_squash)
$ sudo exportfs -rv
exportfs: /etc/exports [2]: Neither 'subtree_check' or 'no_subtree_check' specified for export "host2.some.domain.org:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: incompatible duplicated export entries:
exportfs:       host2.some.domain.org:/some/directory (0x25) [IGNORED]
exportfs:       host2.some.domain.org:/some/directory (0x424)
exportfs: /etc/exports [4]: Neither 'subtree_check' or 'no_subtree_check' specified for export "host2.some.domain.org:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: incompatible duplicated export entries:
exportfs:       host2.some.domain.org:/some/directory (0x421) [IGNORED]
exportfs:       host2.some.domain.org:/some/directory (0x424)
exporting host2.some.domain.org:/some/directory
ERROR(1) $ sudo exportfs -v
/some/directory
                host2.some.domain.org(rw,wdelay,root_squash,no_subtree_check,sec=sys,rw,secure,root_squash,no_all_squash)
```

#### Duplicate export entries with identical options

```bash
$ cat /etc/exports
# /etc/exports: the access control list for filesystems which may be exported
#               to NFS clients.  See exports(5).
#
# Example for NFSv2 and NFSv3:
# /srv/homes       hostname1(rw,sync,no_subtree_check) hostname2(ro,sync,no_subtree_check)
#
# Example for NFSv4:
# /srv/nfs4        gss/krb5i(rw,sync,fsid=0,crossmnt,no_subtree_check)
# /srv/nfs4/homes  gss/krb5i(rw,sync,no_subtree_check)
#

/some/directory     host1.some.domain.org(ro)
/some/directory     host2.some.domain.org(ro)
/some/directory     host2.some.domain.org(ro)
$ sudo exportfs -rv
exportfs: /etc/exports [2]: Neither 'subtree_check' or 'no_subtree_check' specified for export "host1.some.domain.org:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: /etc/exports [3]: Neither 'subtree_check' or 'no_subtree_check' specified for export "host2.some.domain.org:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: /etc/exports [4]: Neither 'subtree_check' or 'no_subtree_check' specified for export "host2.some.domain.org:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: duplicated export entries:
exportfs:       host2.some.domain.org:/some/directory
exportfs:       host2.some.domain.org:/some/directory
exporting host1.some.domain.org:/some/directory
exporting host2.some.domain.org:/some/directory
ERROR(1) $ sudo exportfs
/some/directory
                host1.some.domain.org
/some/directory
                host2.some.domain.org
```

#### Subnet entries do not result in duplicate entry errors

```bash
$ cat /etc/exports
# /etc/exports: the access control list for filesystems which may be exported
#               to NFS clients.  See exports(5).
#
# Example for NFSv2 and NFSv3:
# /srv/homes       hostname1(rw,sync,no_subtree_check) hostname2(ro,sync,no_subtree_check)
#
# Example for NFSv4:
# /srv/nfs4        gss/krb5i(rw,sync,fsid=0,crossmnt,no_subtree_check)
# /srv/nfs4/homes  gss/krb5i(rw,sync,no_subtree_check)
#

/some/directory     192.168.100.100/16(ro)
/some/directory     192.168.100.100(ro)
$ sudo exportfs -rv
exportfs: /etc/exports [2]: Neither 'subtree_check' or 'no_subtree_check' specified for export "192.168.100.100/16:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exportfs: /etc/exports [3]: Neither 'subtree_check' or 'no_subtree_check' specified for export "192.168.100.100:/some/directory".
  Assuming default behaviour ('no_subtree_check').
  NOTE: this default has changed since nfs-utils version 1.0.x

exporting 192.168.100.100:/some/directory
exporting 192.168.100.100/16:/some/directory
$ sudo exportfs
/some/directory
                192.168.100.100
/some/directory
                192.168.100.100/16
```

## Other Information

### How to force a remote user to appear as a different user

This can be accomplished by setting the following options on the export entry:
- Add `all_squash` to the list of export options
- Set the export options, `anonuid` and `anongid`, to the target UID and GID respectively

For example:
```bash
/data   10.0.100.200(ro,sync,no_subtree_check,all_squash,anonuid=1500,anongid=1500)
```
This line means, any user (including root) accessing `/data` via the NFS server from `10.0.100.200` will be recognized as the user having a UID of `1500` and GID of `1500`.

Information obtained using the following sources:
- `man 5 exports`
    - See section entitled **User ID Mapping**
- https://serverfault.com/questions/539267/nfs-share-with-root-for-anonuid-anongid/615624#615624

