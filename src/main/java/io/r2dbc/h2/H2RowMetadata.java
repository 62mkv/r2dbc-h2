/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.h2;

import io.r2dbc.h2.codecs.Codecs;
import io.r2dbc.h2.util.Assert;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.RowMetadata;
import org.h2.result.ResultInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of {@link RowMetadata} for an H2 database.
 */
public final class H2RowMetadata implements RowMetadata {

    private final List<H2ColumnMetadata> columnMetadatas;

    private final Map<String, H2ColumnMetadata> nameKeyedColumnMetadatas;

    H2RowMetadata(List<H2ColumnMetadata> columnMetadatas) {
        this.columnMetadatas = Assert.requireNonNull(columnMetadatas, "columnMetadatas must not be null");

        this.nameKeyedColumnMetadatas = getNameKeyedColumnMetadatas(columnMetadatas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        H2RowMetadata that = (H2RowMetadata) o;
        return Objects.equals(this.columnMetadatas, that.columnMetadatas);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code identifier} does not correspond to a column
     */
    @Override
    public ColumnMetadata getColumnMetadata(Object identifier) {
        Assert.requireNonNull(identifier, "identifier must not be null");

        if (identifier instanceof Integer) {
            return getColumnMetadata((Integer) identifier);
        } else if (identifier instanceof String) {
            return getColumnMetadata((String) identifier);
        }

        throw new IllegalArgumentException(String.format("Identifier '%s' is not a valid identifier. Should either be an Integer index or a String column name.", identifier));
    }

    @Override
    public List<H2ColumnMetadata> getColumnMetadatas() {
        return Collections.unmodifiableList(this.columnMetadatas);
    }

    @Override
    public Collection<String> getColumnNames() {
        return this.columnMetadatas.stream()
            .map(H2ColumnMetadata::getName)
            .collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.columnMetadatas);
    }

    @Override
    public String toString() {
        return "H2RowMetadata{" +
            "columnMetadatas=" + this.columnMetadatas +
            ", nameKeyedColumnMetadatas=" + this.nameKeyedColumnMetadatas +
            '}';
    }

    static H2RowMetadata toRowMetadata(Codecs codecs, ResultInterface result) {
        Assert.requireNonNull(codecs, "codecs must not be null");
        Assert.requireNonNull(result, "result must not be null");

        return new H2RowMetadata(getColumnMetadatas(codecs, result));
    }

    private static List<H2ColumnMetadata> getColumnMetadatas(Codecs codecs, ResultInterface result) {
        List<H2ColumnMetadata> columnMetadatas = new ArrayList<>(result.getVisibleColumnCount());

        for (int i = 0; i < result.getVisibleColumnCount(); i++) {
            columnMetadatas.add(H2ColumnMetadata.toColumnMetadata(codecs, result, i));
        }

        return columnMetadatas;
    }

    private ColumnMetadata getColumnMetadata(Integer index) {
        if (index >= this.columnMetadatas.size()) {
            throw new IllegalArgumentException(String.format("Column index %d is larger than the number of columns %d", index, this.columnMetadatas.size()));
        }

        return this.columnMetadatas.get(index);
    }

    private ColumnMetadata getColumnMetadata(String name) {
        if (!this.nameKeyedColumnMetadatas.containsKey(name)) {
            throw new IllegalArgumentException(String.format("Column name '%s' does not exist in column names %s", name, this.nameKeyedColumnMetadatas.keySet()));
        }

        return this.nameKeyedColumnMetadatas.get(name);
    }

    private Map<String, H2ColumnMetadata> getNameKeyedColumnMetadatas(List<H2ColumnMetadata> columnMetadatas) {
        Map<String, H2ColumnMetadata> nameKeyedColumnMetadatas = new HashMap<>(columnMetadatas.size());

        for (H2ColumnMetadata columnMetadata : columnMetadatas) {
            nameKeyedColumnMetadatas.put(columnMetadata.getName(), columnMetadata);
        }

        return nameKeyedColumnMetadatas;
    }
}
