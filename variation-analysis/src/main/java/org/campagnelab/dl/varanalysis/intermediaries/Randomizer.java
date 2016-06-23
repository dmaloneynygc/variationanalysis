package org.campagnelab.dl.varanalysis.intermediaries;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.hadoop.fs.FileUtil;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.dl.varanalysis.storage.RecordReader;
import org.campagnelab.dl.varanalysis.storage.RecordWriter;
import it.unimi.dsi.util.XorShift128PlusRandom;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 *
 * the randomizer object iterates over a parquet file and randomizes the order of records in batches
 *
 * Created by rct66 on 5/18/16.
 * @author rct66
 */
public class Randomizer extends Intermediary{
    public static final int MAX_ELEMENTS = 20000;
    static private Logger LOG = LoggerFactory.getLogger(Randomizer.class);


    public static void main (String[] args) throws IOException{

        //randomize
        new Randomizer().executeOver(args[0],args[1]);
    }

    public void execute(String inPath, String outPath, int blockSize, int pageSize) throws IOException {
        try {
            RecordReader reader = new RecordReader(inPath);
            RecordWriter writer = new RecordWriter(outPath,blockSize,pageSize,true);
            Random rand = new XorShift128PlusRandom();

            //set up logger
            ProgressLogger pgRead = new ProgressLogger(LOG);
            pgRead.itemsName = "read";
            pgRead.expectedUpdates = reader.getTotalRecords();
            pgRead.displayFreeMemory = true;
            pgRead.start();

            List<BaseInformationRecords.BaseInformation> recList = new ObjectArrayList<BaseInformationRecords.BaseInformation>(MAX_ELEMENTS+1);
            int i = 0;
            for (BaseInformationRecords.BaseInformation rec : reader) {
                recList.add(rec);
                pgRead.lightUpdate();
                i++;
                if (i >= MAX_ELEMENTS){
                    flush(recList,rand,writer);
                    recList.clear();
                    i = 0;
                }

            }
            flush(recList,rand,writer);
            pgRead.stop();
            reader.close();
            writer.close();



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void flush(List<BaseInformationRecords.BaseInformation> recList, Random rand, RecordWriter writer) throws IOException {
        //System.out.println(rec.getSamples(0).getFormattedCounts());
        Collections.shuffle(recList,rand);

        //set up logger2
        ProgressLogger pgWrite = new ProgressLogger(LOG);
        pgWrite.itemsName = "write";
        pgWrite.expectedUpdates = MAX_ELEMENTS;
        pgWrite.displayFreeMemory = true;
        pgWrite.start();
        for ( BaseInformationRecords.BaseInformation recWrite : recList){
            writer.writeRecord(recWrite);
            pgWrite.lightUpdate();
        }
        pgWrite.stop();
    }

}
