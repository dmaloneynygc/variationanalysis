package org.campagnelab.dl.varanalysis.learning.iterators;

import org.apache.commons.compress.utils.IOUtils;
import org.campagnelab.dl.model.utils.mappers.EfficientFeatureMapper;
import org.campagnelab.dl.model.utils.mappers.EfficientLabelMapper;
import org.campagnelab.dl.model.utils.mappers.FeatureMapper;
import org.campagnelab.dl.model.utils.mappers.LabelMapper;
import org.campagnelab.dl.varanalysis.learning.DomainDescriptor;
import org.campagnelab.dl.varanalysis.learning.TrainModel;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.dl.varanalysis.storage.RecordReader;
import org.campagnelab.goby.baseinfo.SequenceBaseInformationReader;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An iterator compatible with the DeepLearning4J framework that can iterate a BaseInformation file
 * and generate features suitable to train the neural net. The iterator can be constructed with a
 * dataset filename and an instance of FeatureCalculator.
 * <p>
 * Created by fac2003 on 5/21/16.
 *
 * @author Fabien Campagne
 */
public abstract class RecordIterator<RecordType> implements MultiDataSetIterator {
    private final DomainDescriptor domainDescriptor;
    private int[] labelStride;
    private int[] featureStride;
    protected long totalExamples;
    private String inputFilename;
    private RecordType nextPosRecord;
    protected long cursor = 0;
    protected int batchSize = 32;
    private MultiDataSetPreProcessor preProcessor;
    private long numRecords;

    public RecordIterator(String inputFilename, int batchSize, DomainDescriptor domainDescriptor) throws IOException {
        this(domainDescriptor);
        this.inputFilename = inputFilename;
        this.batchSize = batchSize;

        this.totalExamples = getNumRecords();

    }

    public String getInputFilename() {
        return inputFilename;
    }

    abstract public String getBasename();

    protected RecordIterator(final DomainDescriptor domainDescriptor) {
        this.domainDescriptor = domainDescriptor;
        this.inputFilename = null;

    }


    public MultiDataSet next(int batchSize) {

        // allocate a new dataset with batchSize records and fill it with features and labels.

        int size = Math.min(batchSize, (int) remainingExamples());

        // allocate features and labels for the entire dataset:
        // dimension 0 = number of examples in minibatch
        // dimension 1 = number of features per record.

        //size changed from batchSize. huge batchSize values useful for tests
        INDArray inputs[] = null;//= Nd4j.zeros(size, featureMapper.numberOfFeatures());
        INDArray labels[] = null;//= Nd4j.zeros(size, labelMapper.numberOfLabels());
        int index = 0;

        for (String input : domainDescriptor.getComputationalGraph().getInputNames()) {

            inputs[index++] = Nd4j.zeros(domainDescriptor.getInputShape(size, input));

        }
        index = 0;
        for (String label : domainDescriptor.getComputationalGraph().getOutputNames()) {
            labels[index++] = Nd4j.zeros(domainDescriptor.getLabelShape(size, label));
        }
        int numInputs = domainDescriptor.getComputationalGraph().getInputNames().length;
        int numOutputs = domainDescriptor.getComputationalGraph().getOutputNames().length;
        FeatureMapper[] featureMappers = new FeatureMapper[numInputs];
        LabelMapper[] labelMappers = new LabelMapper[numOutputs];
        for (int i = 0; i < size; i++) {

            if (hasNextRecord()) {
                // fill in features and labels for a given record i:
                RecordType record = nextRecord();
                for (int j = 0; j < numInputs; j++) {
                    featureMappers[j].prepareToNormalize(record, i);
                    featureMappers[j].mapFeatures(record, inputs[j], i);
                }
                for (int j = 0; j < numOutputs; j++) {
                    labelMappers[j].mapLabels(record, labels[j], i);
                }
            } else {
                // some records may be empty in the very last minibatch at the end of the iterator.
            }

        }
        final MultiDataSet result = new org.nd4j.linalg.dataset.MultiDataSet(inputs, labels);
        preProcessor.preProcess(result);
        return result;
    }

    @Override
    public void setPreProcessor(MultiDataSetPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    private long remainingExamples() {
        return totalExamples - cursor;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }


    public boolean asyncSupported() {
        return false;
    }

    @Override
    abstract public void reset();


    @Override
    public boolean hasNext() {
        if (nextPosRecord != null) {
            return true;
        }
        nextPosRecord = nextRecord();
        return nextPosRecord != null;
    }


    @Override
    public MultiDataSet next() {
        if (hasNext()) {
            return next(batchSize);

        } else throw new NoSuchElementException();
    }

    /**
     * Check if there is a next single PosRecord.
     *
     * @return
     */
    public abstract boolean hasNextRecord();

    /**
     * Return the next single PosRecord.
     *
     * @return the next available record, or throws NoSuchElementException if there are no more records.
     */
    public abstract RecordType nextRecord();

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported by this iterator.");
    }


    public abstract long getNumRecords();
}
