/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2018 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package it.eng.spagobi.tools.dataset.strategy;

import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.common.datastore.Field;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.datastore.IRecord;
import it.eng.spagobi.tools.dataset.common.datastore.Record;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.metasql.query.SelectQuery;
import it.eng.spagobi.tools.dataset.metasql.query.item.Filter;
import it.eng.spagobi.tools.dataset.metasql.query.item.Projection;
import it.eng.spagobi.tools.dataset.metasql.query.item.Sorting;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.database.DataBaseException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractJdbcEvaluationStrategy extends AbstractEvaluationStrategy {

    private static final Logger logger = Logger.getLogger(AbstractJdbcEvaluationStrategy.class);

    public AbstractJdbcEvaluationStrategy(IDataSet dataSet) {
        super(dataSet);
    }

    @Override
    public IDataStore executeQuery(List<Projection> projections, Filter filter, List<Projection> groups, List<Sorting> sortings, List<Projection> summaryRowProjections, int offset, int fetchSize, int maxRowCount) {
        logger.debug("IN");

        IDataStore pagedDataStore;

        try {
            SelectQuery selectQuery = new SelectQuery(dataSet).selectDistinct().select(projections).from(getTableName()).where(filter).groupBy(groups).orderBy(sortings);
            pagedDataStore = getDataSource().executeStatement(selectQuery, offset, fetchSize, maxRowCount);

            if (summaryRowProjections != null && !summaryRowProjections.isEmpty()) {
                String summaryRowQuery = new SelectQuery(dataSet).selectDistinct().select(summaryRowProjections).from(getTableName()).where(filter).toSql(getDataSource());
                IDataStore summaryRowDataStore = getDataSource().executeStatement(summaryRowQuery, -1, -1, maxRowCount);
                appendSummaryRowToPagedDataStore(projections, summaryRowProjections, pagedDataStore, summaryRowDataStore);
            }

            pagedDataStore.setCacheDate(getDate());
        } catch (DataBaseException e) {
            throw new RuntimeException(e);
        } finally {
            logger.debug("OUT");
        }
        return pagedDataStore;
    }

    protected abstract String getTableName() throws DataBaseException;

    protected abstract IDataSource getDataSource();

    private void appendSummaryRowToPagedDataStore(List<Projection> projections, List<Projection> summaryRowProjections, IDataStore pagedDataStore,
                                                  IDataStore summaryRowDataStore) {
        IMetaData pagedMetaData = pagedDataStore.getMetaData();
        IMetaData summaryRowMetaData = summaryRowDataStore.getMetaData();

        if (pagedMetaData.getFieldCount() >= summaryRowMetaData.getFieldCount()) {
            // calc a map for summaryRowProjections -> projections
            Map<Integer, Integer> projectionToSummaryRowProjection = new HashMap<>();
            for (int i = 0; i < summaryRowProjections.size(); i++) {
                Projection summaryRowProjection = summaryRowProjections.get(i);
                for (int j = 0; j < projections.size(); j++) {
                    Projection projection = projections.get(j);
                    String projectionAlias = projection.getAlias();
                    if (summaryRowProjection.getAlias().equals(projectionAlias) || summaryRowProjection.getName().equals(projectionAlias)) {
                        projectionToSummaryRowProjection.put(j, i);
                        break;
                    }
                }
            }

            // append summary row
            IRecord summaryRowRecord = summaryRowDataStore.getRecordAt(0);
            Record newRecord = new Record();
            for (int projectionIndex = 0; projectionIndex < pagedMetaData.getFieldCount(); projectionIndex++) {
                Field field = new Field(null);
                if (projectionToSummaryRowProjection.containsKey(projectionIndex)) {
                    Integer summaryRowIndex = projectionToSummaryRowProjection.get(projectionIndex);
                    Object value = summaryRowRecord.getFieldAt(summaryRowIndex).getValue();
                    field.setValue(value);
                }
                newRecord.appendField(field);
            }
            pagedDataStore.appendRecord(newRecord);

            // copy metadata from summary row
            for (Integer projectionIndex : projectionToSummaryRowProjection.keySet()) {
                Integer summaryRowIndex = projectionToSummaryRowProjection.get(projectionIndex);
                pagedMetaData.getFieldMeta(projectionIndex).setType(summaryRowMetaData.getFieldType(summaryRowIndex));
            }
        }
    }
}
