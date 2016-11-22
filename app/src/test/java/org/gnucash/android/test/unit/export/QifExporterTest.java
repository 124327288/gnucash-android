/*
 * Copyright (c) 2016 Àlex Magaz Graça <alexandre.magaz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnucash.android.test.unit.export;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.model.Book;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class,
        sdk = 21,
        packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class QifExporterTest {
    private SQLiteDatabase mDb;

    @Before
    public void setUp() throws Exception {
        BookDbHelper bookDbHelper = new BookDbHelper(GnuCashApplication.getAppContext());
        BooksDbAdapter booksDbAdapter = new BooksDbAdapter(bookDbHelper.getWritableDatabase());
        Book testBook = new Book("testRootAccountUID");
        booksDbAdapter.addRecord(testBook);
        DatabaseHelper databaseHelper =
                new DatabaseHelper(GnuCashApplication.getAppContext(), testBook.getUID());
        mDb = databaseHelper.getWritableDatabase();
    }

    /**
     * When there aren't new or modified transactions, the QIF exporter
     * shouldn't create any file.
     */
    @Test
    public void testWithNoTransactionsToExport_shouldNotCreateAnyFile(){
        ExportParams exportParameters = new ExportParams(ExportFormat.QIF);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);
        QifExporter exporter = new QifExporter(exportParameters, mDb);
        assertThat(exporter.generateExport()).isEmpty();
    }
}