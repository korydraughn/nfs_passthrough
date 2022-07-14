package org.irods.nfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.ExportTable;
import org.dcache.nfs.FsExport;
import org.dcache.nfs.HostEntryComparator;
import org.dcache.nfs.v4.xdr.layouttype4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

public class DynamicExportFile implements ExportTable
{
    private static final Logger log = LoggerFactory.getLogger(ExportFile.class);

    private volatile ImmutableMultimap<Integer, FsExport> exports;
    private final Callable<URI[]> exportFileProvider;
    private List<String> malformedExportEntries;

    /**
     * Construct server export table from a given file.
     *
     * @param file the file that contains the export table.
     * @throws IOException
     */
    public DynamicExportFile(File file) throws IOException
    {
        this(file, null);
    }

    /**
     * Construct server export table from a given file. The {@code dir} points
     * to a directory with extra export tables. Only files ending in .exports
     * are considered. Files beginning with a dot are ignored.
     *
     * @param file the file that contains the export table.
     * @param dir the directory with extra export tables.
     * @throws IOException
     */
    public DynamicExportFile(File file, File dir) throws IOException
    {
        exportFileProvider = () -> {
            if (dir != null && dir.exists()) {
                Preconditions.checkArgument(dir.isDirectory(), dir.getAbsolutePath() + " exist but not a directory");

                File[] files = dir.listFiles((File d, String n) -> n.endsWith(".exports") && n.charAt(0) != '.');
                URI[] exportFiles = new URI[files.length + 1];
                exportFiles[0] = file.toURI();

                for (int i = 0; i < files.length; i++) {
                    exportFiles[i + 1] = files[i].toURI();
                }

                return exportFiles;
            }

            return new URI[] {file.toURI()};
        };

        malformedExportEntries = new ArrayList<>();

        rescan();
    }

    public DynamicExportFile(URI uri) throws IOException
    {
        exportFileProvider = () -> {
            return new URI[] {uri};
        };

        malformedExportEntries = new ArrayList<>();

        rescan();
    }

    public DynamicExportFile(Reader reader) throws IOException
    {
        exportFileProvider = () -> {
            throw new IllegalStateException("exports uri not set, rescan impossible");
        };

        malformedExportEntries = new ArrayList<>();
        exports = parse(reader, malformedExportEntries);
    }

    @Override public Stream<FsExport> exports()
    {
        return exports.values().stream();
    }

    private static ImmutableMultimap<Integer, FsExport> parse(Reader reader, List<String> malformedExportEntries)
        throws IOException
    {
        List<String> lines;
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            lines = bufferedReader.lines().collect(Collectors.toList());
        }
        return parseExportLines(lines, malformedExportEntries);
    }

    private static ImmutableMultimap<Integer, FsExport> parse(List<String> malformedExportEntries, URI... exportFiles)
        throws IOException
    {
        ImmutableListMultimap.Builder<Integer, FsExport> exportsBuilder = ImmutableListMultimap.builder();
        for (URI exportFile : exportFiles) {
            List<String> lines = Files.readAllLines(Paths.get(exportFile));
            Multimap<Integer, FsExport> export = parseExportLines(lines, malformedExportEntries);
            exportsBuilder.putAll(export);
        }

        // Sort in reverse order to get smallest network first.
        return exportsBuilder.orderValuesBy(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client))
            .build();
    }

    private static ImmutableMultimap<Integer, FsExport> parseExportLines(Iterable<String> lines,
                                                                         List<String> malformedExportEntries)
        throws IOException
    {
        ImmutableListMultimap.Builder<Integer, FsExport> exportsBuilder = ImmutableListMultimap.builder();

        for (String line : lines) {
            line = line.trim();

            if (line.length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.charAt(0) != '/') {
                log.warn("Ignoring entry with non absolute export path: " + line);
                continue;
            }

            int pathEnd = line.indexOf(' ');

            String path;
            if (pathEnd < 0) {
                FsExport export = new FsExport.FsExportBuilder().build(line);
                exportsBuilder.put(export.getIndex(), export);
                continue;
            }
            else {
                path = line.substring(0, pathEnd);
            }

            Splitter splitter = Splitter.on(' ').omitEmptyStrings().trimResults();

            for (String hostAndOptions : splitter.split(line.substring(pathEnd + 1))) {
                try {
                    FsExport.FsExportBuilder exportBuilder = new FsExport.FsExportBuilder();

                    Iterator<String> s = Splitter.on(CharMatcher.anyOf("(,)"))
                                             .omitEmptyStrings()
                                             .trimResults()
                                             .split(hostAndOptions)
                                             .iterator();

                    String host = s.next();

                    exportBuilder.forClient(host);
                    while (s.hasNext()) {
                        String option = s.next();

                        if (option.equals("rw")) {
                            exportBuilder.rw();
                            continue;
                        }

                        if (option.equals("ro")) {
                            exportBuilder.ro();
                            continue;
                        }

                        if (option.equals("root_squash")) {
                            exportBuilder.notTrusted();
                            continue;
                        }

                        if (option.equals("no_root_squash")) {
                            exportBuilder.trusted();
                            continue;
                        }

                        if (option.equals("acl")) {
                            exportBuilder.withAcl();
                            continue;
                        }

                        if (option.equals("noacl") || option.equals("no_acl")) {
                            exportBuilder.withoutAcl();
                            continue;
                        }

                        if (option.equals("all_squash")) {
                            exportBuilder.allSquash();
                            continue;
                        }

                        if (option.startsWith("sec=")) {
                            String secFlavor = option.substring(4);
                            exportBuilder.withSec(FsExport.Sec.valueOf(secFlavor.toUpperCase()));
                            continue;
                        }

                        if (option.startsWith("anonuid=")) {
                            int anonuid = Integer.parseInt(option.substring(8));
                            exportBuilder.withAnonUid(anonuid);
                            continue;
                        }

                        if (option.startsWith("anongid=")) {
                            int anongid = Integer.parseInt(option.substring(8));
                            exportBuilder.withAnonGid(anongid);
                            continue;
                        }

                        if (option.equals("dcap")) {
                            exportBuilder.withDcap();
                            continue;
                        }

                        if (option.equals("no_dcap")) {
                            exportBuilder.withoutDcap();
                            continue;
                        }

                        if (option.equals("all_root")) {
                            exportBuilder.withAllRoot();
                            continue;
                        }

                        if (option.equals("pnfs")) {
                            exportBuilder.withPnfs();
                            continue;
                        }

                        if (option.equals("nopnfs") || option.equals("no_pnfs")) {
                            exportBuilder.withoutPnfs();
                            continue;
                        }

                        if (option.startsWith("lt=")) {
                            Iterable<String> lt = Splitter.on(":").omitEmptyStrings().split(option.substring(3));

                            StreamSupport.stream(lt.spliterator(), false)
                                .map(String::toUpperCase)
                                .map(t -> "LAYOUT4_" + t)
                                .map(layouttype4::valueOf)
                                .forEach(exportBuilder::withLayoutType);
                            continue;
                        }

                        if (option.equals("secure")) {
                            exportBuilder.withPrivilegedClientPort();
                            continue;
                        }

                        if (option.equals("insecure")) {
                            exportBuilder.withoutPrivilegedClientPort();
                            continue;
                        }

                        throw new IllegalArgumentException("Unsupported option: " + option);
                    }

                    FsExport export = exportBuilder.build(path);
                    exportsBuilder.put(export.getIndex(), export);
                }
                catch (IllegalArgumentException e) {
                    log.error("Invalid export entry [" + hostAndOptions + "] : " + e.getMessage());
                    malformedExportEntries.add(hostAndOptions);
                }
            }
        }

        // Sort in reverse order to get smallest network first.
        return exportsBuilder.orderValuesBy(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client))
            .build();
    }

    @Override public FsExport getExport(String path, InetAddress client)
    {
        String normalizedPath = FsExport.normalize(path);
        return getExport(FsExport.getExportIndex(normalizedPath), client);
    }

    @Override public FsExport getExport(int index, InetAddress client)
    {
        for (FsExport export : exports.get(index)) {
            if (export.isAllowed(client)) {
                return export;
            }
        }

        return null;
    }

    @Override public Stream<FsExport> exports(InetAddress client)
    {
        return exports.values()
            .stream()
            .filter(e -> e.isAllowed(client))
            .sorted(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client));
    }

    public final void rescan() throws IOException
    {
        malformedExportEntries.clear();

        try {
            exports = parse(malformedExportEntries, exportFileProvider.call());
        }
        catch (Exception e) {
            Throwables.throwIfInstanceOf(e, IOException.class);
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException("Unhandled exception", e);
        }
    }

    public final List<String> getMalformedExportEntries()
    {
        return Collections.unmodifiableList(malformedExportEntries);
    }
}
