package org.campagnelab.dl.varanalysis.storage;


import com.codahale.metrics.MetricRegistryListener;
import com.google.protobuf.TextFormat;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.it.unimi.dsi.fastutil.ints.IntSet;
import org.campagnelab.dl.varanalysis.intermediaries.Mutator;
import org.campagnelab.dl.varanalysis.intermediaries.Randomizer;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.eclipse.jetty.util.IO;

import java.io.IOException;
import java.util.List;
import java.util.Set;


/**
 * Currently holds the main method. Jar takes two arguments
 * <p>
 * java -jar var-analysis.jar process /recordsPath/to/genotypes.parquet
 * creates mutated and randomized parquet file, then prints the latter
 * <p>
 * java -jar var-analysis.jar print /recordsPath/to/genotypes.parquet
 * prints the parquet file as is
 * <p>
 * Also, this jar should be a resource for Goby to output variations as a parquet file using its AvroVariationOutputFormat
 * class.
 * Created by rct66 on 5/17/16.
 *
 * @author rct66
 */
public class ParquetPrinter {

    String path;
    boolean focusPrint = false;
    private int refIndex;
    private int position;
    private boolean customPosOnly = false;

    private int[] customPos = {
            67478327,
            62361697,
            65888432,
            21317812,
            51773638,
            123075887,
            42032422,
            106733586,
            96831233,
            16295959,
            18830766,
            81050949,
            6828090,
            16899837,
            40149296,
            55267530,
    };
    private Set<Integer> posSet = new IntOpenHashSet(customPos);

    public static void main(String[] args) throws IOException {
        if (args.length<1) {
            System.err.println("usage: printer <parquet-file> [focus-ref-index focus-position]");
            System.exit(1);
        }
        ParquetPrinter parquetPrinter = new ParquetPrinter(args[0]);
        if (args.length >= 3) {
            // will only print the record(s) matching a specific position:
            parquetPrinter.setFocusOnPosition(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            System.out.println("Scanning for ");
        }
        parquetPrinter.print();


    }

    private void setFocusOnPosition(int refIndex, int position) {
        focusPrint = true;
        this.refIndex = refIndex;
        this.position = position;
    }


    public ParquetPrinter(String path) {
        this.path = path;
    }

    private void recordPrinter(BaseInformationRecords.BaseInformation base) throws IOException {
        TextFormat.print(base, System.out);
    }

    public void print() {
        try {
            RecordReader reader = new RecordReader(path);
            for (BaseInformationRecords.BaseInformation base : reader) {
                if (!(focusPrint || customPosOnly) ||
                        (base.getReferenceIndex() == refIndex && base.getPosition() == position) || (posSet.contains(base.getPosition()))) {
                    recordPrinter(base);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}