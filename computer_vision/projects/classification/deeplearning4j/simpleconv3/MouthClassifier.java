
// Copyright 2019 longpeng2008. All Rights Reserved.
// Licensed under the Apache License, Version 2.0 (the "License");
// If you find any problem,please contact us
//
//     longpeng2008to2012@gmail.com 
//
// or create issues
// =============================================================================
package org.deeplearning4j.examples.convolution.mnist;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;

/**
 * Handwritten digits image classification on MNIST dataset (99% accuracy).
 * This example will download 15 Mb of data on the first run.
 * Supervised learning best modeled by CNN.
 *
 * @author hanlon
 * @author agibsonccc
 * @author fvaleri
 */
public class MouthClassifier {

    private static final Logger log = LoggerFactory.getLogger(MouthClassifier.class);
    private static final String inputDataDir = "/Users/huhujun/yousancv/classification/LongPeng_ML_Course-master/datas/mouth/";
    private static final String modelDir = "/Users/huhujun/yousancv/classification/LongPeng_ML_Course-master/projects/classification/deeplearning4j/";

    public static void main(String[] args) throws Exception {
        log.info(modelDir);
        int height = 48;  // ??????????????????
        int width = 48;   // ??????????????????
        int channels = 3; // ?????????????????????
        int outputNum = 2; // 2??????
        int batchSize = 64;
        int nEpochs = 100;
        int seed = 1234;
        Random randNumGen = new Random(seed);

        // ????????????????????????
        File trainData = new File(inputDataDir + "/train");
        FileSplit trainSplit = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator(); // parent path as the image label
        ImageRecordReader trainRR = new ImageRecordReader(height, width, channels, labelMaker);
        trainRR.initialize(trainSplit);
        DataSetIterator trainIter = new RecordReaderDataSetIterator(trainRR, batchSize, 1, outputNum);

        // ????????????0-255?????????0-1 (???min-max?????????????????????)
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(trainIter);
        trainIter.setPreProcessor(scaler);

        // ????????????????????????
        File testData = new File(inputDataDir + "/test");
        FileSplit testSplit = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        ImageRecordReader testRR = new ImageRecordReader(height, width, channels, labelMaker);
        testRR.initialize(testSplit);
        DataSetIterator testIter = new RecordReaderDataSetIterator(testRR, batchSize, 1, outputNum);
        testIter.setPreProcessor(scaler); // same normalization for better results

        log.info("Network configuration and training...");
        // ???????????????????????????
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(0.0005)
            .updater(new Adam(0.0001))
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(0, new ConvolutionLayer.Builder(3, 3)
                .nIn(channels)
                .stride(2, 2)
                .nOut(12)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(1, new BatchNormalization.Builder()
                .nIn(12)
                .nOut(12)
                .build())
            .layer(2, new ConvolutionLayer.Builder(3, 3)
                .nIn(12)
                .stride(2, 2)
                .nOut(24)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(3, new BatchNormalization.Builder()
                .nIn(24)
                .nOut(24)
                .build())
            .layer(4, new ConvolutionLayer.Builder(3, 3)
                .nIn(24)
                .stride(2, 2)
                .nOut(48)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(5, new BatchNormalization.Builder()
                .nIn(48)
                .nOut(48)
                .build())
            .layer(6, new DenseLayer.Builder().activation(Activation.RELU)
                .nOut(128).build())
            .layer(7, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(outputNum)
                .activation(Activation.SOFTMAX)
                .build())
            .setInputType(InputType.convolutionalFlat(48, 48, 3)) // InputType.convolutional for normal image
            .backprop(true).pretrain(false).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        //???????????????????????????,????????????UI??????
        UIServer uiServer = UIServer.getInstance();
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????
        StatsStorage statsStorage = new InMemoryStatsStorage();         //????????? new FileStatsStorage(File)???????????????????????????????????????????????????
        //???StatsStorage?????????????????????????????????StatsStorage???????????????????????????
        uiServer.attach(statsStorage);
        //????????????StatsListener???????????????????????????????????????
        net.setListeners(new StatsListener(statsStorage));

        // ????????????????????????????????????
        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainIter);
            log.info("Completed epoch " + i);
            Evaluation trainEval = net.evaluate(trainIter);
            Evaluation eval = net.evaluate(testIter);
            log.info("train: " + trainEval.precision());
            log.info("val: " + eval.precision());
            trainIter.reset();
            testIter.reset();
        }
        //????????????
        ModelSerializer.writeModel(net, new File(modelDir + "/mouth-model.zip"), true);
    }

}
