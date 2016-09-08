/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.initialization.loadercache;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.gradle.api.internal.hash.Hasher;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.FileUtils;
import org.gradle.internal.classloader.ClassPathSnapshot;
import org.gradle.internal.classloader.ClassPathSnapshotter;
import org.gradle.internal.classpath.ClassPath;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class HashClassPathSnapshotter implements ClassPathSnapshotter {
    private static final Logger LOGGER = Logging.getLogger(HashClassPathSnapshotter.class);

    private final Hasher hasher;

    public HashClassPathSnapshotter(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public ClassPathSnapshot snapshot(ClassPath classPath) {
        final List<String> visitedFilePaths = Lists.newLinkedList();
        final Set<File> visitedDirs = Sets.newLinkedHashSet();
        final List<File> cpFiles = classPath.getAsFiles();
        com.google.common.hash.Hasher checksum = Hashing.md5().newHasher();
        hash(checksum, visitedFilePaths, visitedDirs, cpFiles.iterator());
        return new HashClassPathSnapshot(visitedFilePaths, checksum.hash());
    }

    private void hash(com.google.common.hash.Hasher combinedHash, List<String> visitedFilePaths, Set<File> visitedDirs, Iterator<File> toHash) {
        while (toHash.hasNext()) {
            File file = FileUtils.canonicalize(toHash.next());
            if (file.isDirectory()) {
                if (visitedDirs.add(file)) {
                    //in theory, awkward symbolic links can lead to recursion problems.
                    //TODO - figure out a way to test it. I only tested it 'manually' and the feature is needed.
                    hash(combinedHash, visitedFilePaths, visitedDirs, Iterators.forArray(file.listFiles()));
                }
            } else if (file.isFile()) {
                visitedFilePaths.add(file.getAbsolutePath());
                combinedHash.putBytes(hasher.hash(file).asBytes());
            }
            //else an empty folder - a legit situation
        }
    }

    private static class HashClassPathSnapshot implements ClassPathSnapshot {
        private final List<String> files;
        private final HashCode hash;

        public HashClassPathSnapshot(List<String> files, HashCode hash) {
            assert files != null;
            this.files = files;
            this.hash = hash;
        }

        @Override
        public HashCode getStrongHash() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HashClassPathSnapshot that = (HashClassPathSnapshot) o;

            boolean equalsHash = hash.equals(that.hash);
            boolean equalsFiles = files.equals(that.files);
            boolean equalsFilesWithLog = abstractListEqualsWithLog(files, that.files);
            if (equalsFiles != equalsFilesWithLog) {
                LOGGER.info("[CLC - Equals]   UNEXPECTED - equalsFiles != equalsFilesWithLog: equalsFiles=" + equalsFiles + " equalsFilesWithLog=" + equalsFilesWithLog);
            }

            if (!equalsHash) {
                LOGGER.info("[CLC - Equals]   Objects.equal(this.parent, that.parent) = false : hash = " + hash.toString());
            }
            if (!equalsFiles) {
                LOGGER.info("[CLC - Equals]   files.equals(that.files)) = false");
            }

            return equalsHash && equalsFiles;
        }

        /**
         * Based on AbstractList.equals()
         */
        public <E> boolean abstractListEqualsWithLog(List<E> l, Object o) {
            if (o == this)
                return true;
            if (!(o instanceof List)) {
                LOGGER.info("[CLC - Equals]     !(o instanceof List)): " + o.getClass().getSimpleName());
                return false;
            }

            ListIterator<E> e1 = l.listIterator();
            ListIterator<?> e2 = ((List<?>) o).listIterator();
            while (e1.hasNext() && e2.hasNext()) {
                E o1 = e1.next();
                Object o2 = e2.next();
                if (!(o1==null ? o2==null : o1.equals(o2))) {
                    LOGGER.info("[CLC - Equals]     o1 != o2 : " + "o1=" + o1 + " o2=" + o2);
                    return false;
                }
            }
            boolean lengthDiff = !(e1.hasNext() || e2.hasNext());
            if (!lengthDiff) {
                LOGGER.info("[CLC - Equals]     e1.size() != e2.size() : " + "l1.size()=" + l.size() + " l2.size()=" + ((List<?>) o).size());
            }
            return lengthDiff;
        }

        public String toString() {
            return hash + printFiles();
        }

        private String printFiles() {
            String s = "";
            for (String entry : files) {
                s += entry + ", ";
            }
            return "(" + s + ")";
        }

        @Override
        public int hashCode() {
            int result = files.hashCode();
            result = 31 * result + hash.hashCode();
            return result;
        }
    }
}
