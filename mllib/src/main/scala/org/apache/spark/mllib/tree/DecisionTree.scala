/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.mllib.tree

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.apache.spark.annotation.Experimental
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.Logging
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest.NodeIndexInfo
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.tree.configuration.FeatureType._
import org.apache.spark.mllib.tree.configuration.QuantileStrategy._
import org.apache.spark.mllib.tree.impl._
import org.apache.spark.mllib.tree.impurity.{Impurities, Impurity}
import org.apache.spark.mllib.tree.impurity._
import org.apache.spark.mllib.tree.model._
import org.apache.spark.rdd.RDD
import org.apache.spark.util.random.XORShiftRandom


/**
 * :: Experimental ::
 * A class which implements a decision tree learning algorithm for classification and regression.
 * It supports both continuous and categorical features.
 * @param strategy The configuration parameters for the tree algorithm which specify the type
 *                 of algorithm (classification, regression, etc.), feature type (continuous,
 *                 categorical), depth of the tree, quantile calculation strategy, etc.
 */
@Experimental
class DecisionTree (private val strategy: Strategy) extends Serializable with Logging {

  strategy.assertValid()

  /**
   * Method to train a decision tree model over an RDD
   * @param input Training data: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]]
   * @return DecisionTreeModel that can be used for prediction
   */
  def train(input: RDD[LabeledPoint]): DecisionTreeModel = {
    // Note: random seed will not be used since numTrees = 1.
    val rf = new RandomForest(strategy, numTrees = 1, featureSubsetStrategy = "all", seed = 0)
    val rfModel = rf.train(input)
    rfModel.trees(0)
  }

}

object DecisionTree extends Serializable with Logging {

  /**
   * Method to train a decision tree model.
   * The method supports binary and multiclass classification and regression.
   *
   * Note: Using [[org.apache.spark.mllib.tree.DecisionTree$#trainClassifier]]
   *       and [[org.apache.spark.mllib.tree.DecisionTree$#trainRegressor]]
   *       is recommended to clearly separate classification and regression.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              For classification, labels should take values {0, 1, ..., numClasses-1}.
   *              For regression, labels are real numbers.
   * @param strategy The configuration parameters for the tree algorithm which specify the type
   *                 of algorithm (classification, regression, etc.), feature type (continuous,
   *                 categorical), depth of the tree, quantile calculation strategy, etc.
   * @return DecisionTreeModel that can be used for prediction
  */
  def train(input: RDD[LabeledPoint], strategy: Strategy): DecisionTreeModel = {
    new DecisionTree(strategy).train(input)
  }

  /**
   * Method to train a decision tree model.
   * The method supports binary and multiclass classification and regression.
   *
   * Note: Using [[org.apache.spark.mllib.tree.DecisionTree$#trainClassifier]]
   *       and [[org.apache.spark.mllib.tree.DecisionTree$#trainRegressor]]
   *       is recommended to clearly separate classification and regression.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              For classification, labels should take values {0, 1, ..., numClasses-1}.
   *              For regression, labels are real numbers.
   * @param algo algorithm, classification or regression
   * @param impurity impurity criterion used for information gain calculation
   * @param maxDepth Maximum depth of the tree.
   *                 E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   * @return DecisionTreeModel that can be used for prediction
   */
  def train(
      input: RDD[LabeledPoint],
      algo: Algo,
      impurity: Impurity,
      maxDepth: Int): DecisionTreeModel = {
    val strategy = new Strategy(algo, impurity, maxDepth)
    new DecisionTree(strategy).train(input)
  }

  /**
   * Method to train a decision tree model.
   * The method supports binary and multiclass classification and regression.
   *
   * Note: Using [[org.apache.spark.mllib.tree.DecisionTree$#trainClassifier]]
   *       and [[org.apache.spark.mllib.tree.DecisionTree$#trainRegressor]]
   *       is recommended to clearly separate classification and regression.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              For classification, labels should take values {0, 1, ..., numClasses-1}.
   *              For regression, labels are real numbers.
   * @param algo algorithm, classification or regression
   * @param impurity impurity criterion used for information gain calculation
   * @param maxDepth Maximum depth of the tree.
   *                 E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   * @param numClassesForClassification number of classes for classification. Default value of 2.
   * @return DecisionTreeModel that can be used for prediction
   */
  def train(
      input: RDD[LabeledPoint],
      algo: Algo,
      impurity: Impurity,
      maxDepth: Int,
      numClassesForClassification: Int): DecisionTreeModel = {
    val strategy = new Strategy(algo, impurity, maxDepth, numClassesForClassification)
    new DecisionTree(strategy).train(input)
  }

  /**
   * Method to train a decision tree model.
   * The method supports binary and multiclass classification and regression.
   *
   * Note: Using [[org.apache.spark.mllib.tree.DecisionTree$#trainClassifier]]
   *       and [[org.apache.spark.mllib.tree.DecisionTree$#trainRegressor]]
   *       is recommended to clearly separate classification and regression.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              For classification, labels should take values {0, 1, ..., numClasses-1}.
   *              For regression, labels are real numbers.
   * @param algo classification or regression
   * @param impurity criterion used for information gain calculation
   * @param maxDepth Maximum depth of the tree.
   *                 E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   * @param numClassesForClassification number of classes for classification. Default value of 2.
   * @param maxBins maximum number of bins used for splitting features
   * @param quantileCalculationStrategy  algorithm for calculating quantiles
   * @param categoricalFeaturesInfo Map storing arity of categorical features.
   *                                E.g., an entry (n -> k) indicates that feature n is categorical
   *                                with k categories indexed from 0: {0, 1, ..., k-1}.
   * @return DecisionTreeModel that can be used for prediction
   */
  def train(
      input: RDD[LabeledPoint],
      algo: Algo,
      impurity: Impurity,
      maxDepth: Int,
      numClassesForClassification: Int,
      maxBins: Int,
      quantileCalculationStrategy: QuantileStrategy,
      categoricalFeaturesInfo: Map[Int,Int]): DecisionTreeModel = {
    val strategy = new Strategy(algo, impurity, maxDepth, numClassesForClassification, maxBins,
      quantileCalculationStrategy, categoricalFeaturesInfo)
    new DecisionTree(strategy).train(input)
  }

  /**
   * Method to train a decision tree model for binary or multiclass classification.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              Labels should take values {0, 1, ..., numClasses-1}.
   * @param numClassesForClassification number of classes for classification.
   * @param categoricalFeaturesInfo Map storing arity of categorical features.
   *                                E.g., an entry (n -> k) indicates that feature n is categorical
   *                                with k categories indexed from 0: {0, 1, ..., k-1}.
   * @param impurity Criterion used for information gain calculation.
   *                 Supported values: "gini" (recommended) or "entropy".
   * @param maxDepth Maximum depth of the tree.
   *                 E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   *                  (suggested value: 5)
   * @param maxBins maximum number of bins used for splitting features
   *                 (suggested value: 32)
   * @return DecisionTreeModel that can be used for prediction
   */
  def trainClassifier(
      input: RDD[LabeledPoint],
      numClassesForClassification: Int,
      categoricalFeaturesInfo: Map[Int, Int],
      impurity: String,
      maxDepth: Int,
      maxBins: Int): DecisionTreeModel = {
    val impurityType = Impurities.fromString(impurity)
    train(input, Classification, impurityType, maxDepth, numClassesForClassification, maxBins, Sort,
      categoricalFeaturesInfo)
  }

  /**
   * Java-friendly API for [[org.apache.spark.mllib.tree.DecisionTree$#trainClassifier]]
   */
  def trainClassifier(
      input: JavaRDD[LabeledPoint],
      numClassesForClassification: Int,
      categoricalFeaturesInfo: java.util.Map[java.lang.Integer, java.lang.Integer],
      impurity: String,
      maxDepth: Int,
      maxBins: Int): DecisionTreeModel = {
    trainClassifier(input.rdd, numClassesForClassification,
      categoricalFeaturesInfo.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap,
      impurity, maxDepth, maxBins)
  }

  /**
   * Method to train a decision tree model for regression.
   *
   * @param input Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
   *              Labels are real numbers.
   * @param categoricalFeaturesInfo Map storing arity of categorical features.
   *                                E.g., an entry (n -> k) indicates that feature n is categorical
   *                                with k categories indexed from 0: {0, 1, ..., k-1}.
   * @param impurity Criterion used for information gain calculation.
   *                 Supported values: "variance".
   * @param maxDepth Maximum depth of the tree.
   *                 E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   *                  (suggested value: 5)
   * @param maxBins maximum number of bins used for splitting features
   *                 (suggested value: 32)
   * @return DecisionTreeModel that can be used for prediction
   */
  def trainRegressor(
      input: RDD[LabeledPoint],
      categoricalFeaturesInfo: Map[Int, Int],
      impurity: String,
      maxDepth: Int,
      maxBins: Int): DecisionTreeModel = {
    val impurityType = Impurities.fromString(impurity)
    train(input, Regression, impurityType, maxDepth, 0, maxBins, Sort, categoricalFeaturesInfo)
  }

  /**
   * Java-friendly API for [[org.apache.spark.mllib.tree.DecisionTree$#trainRegressor]]
   */
  def trainRegressor(
      input: JavaRDD[LabeledPoint],
      categoricalFeaturesInfo: java.util.Map[java.lang.Integer, java.lang.Integer],
      impurity: String,
      maxDepth: Int,
      maxBins: Int): DecisionTreeModel = {
    trainRegressor(input.rdd,
      categoricalFeaturesInfo.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap,
      impurity, maxDepth, maxBins)
  }

  /**
   * Get the node index corresponding to this data point.
   * This function mimics prediction, passing an example from the root node down to a leaf
   * or unsplit node; that node's index is returned.
   *
   * @param node  Node in tree from which to classify the given data point.
   * @param binnedFeatures  Binned feature vector for data point.
   * @param bins possible bins for all features, indexed (numFeatures)(numBins)
   * @param unorderedFeatures  Set of indices of unordered features.
   * @return  Leaf index if the data point reaches a leaf.
   *          Otherwise, last node reachable in tree matching this example.
   *          Note: This is the global node index, i.e., the index used in the tree.
   *                This index is different from the index used during training a particular
   *                group of nodes on one call to [[findBestSplits()]].
   */
  private def predictNodeIndex(
      node: Node,
      binnedFeatures: Array[Int],
      bins: Array[Array[Bin]],
      unorderedFeatures: Set[Int]): Int = {
    if (node.isLeaf || node.split.isEmpty) {
      // Node is either leaf, or has not yet been split.
      node.id
    } else {
      val featureIndex = node.split.get.feature
      val splitLeft = node.split.get.featureType match {
        case Continuous => {
          val binIndex = binnedFeatures(featureIndex)
          val featureValueUpperBound = bins(featureIndex)(binIndex).highSplit.threshold
          // bin binIndex has range (bin.lowSplit.threshold, bin.highSplit.threshold]
          // We do not need to check lowSplit since bins are separated by splits.
          featureValueUpperBound <= node.split.get.threshold
        }
        case Categorical => {
          val featureValue = binnedFeatures(featureIndex)
          node.split.get.categories.contains(featureValue)
        }
        case _ => throw new RuntimeException(s"predictNodeIndex failed for unknown reason.")
      }
      if (node.leftNode.isEmpty || node.rightNode.isEmpty) {
        // Return index from next layer of nodes to train
        if (splitLeft) {
          Node.leftChildIndex(node.id)
        } else {
          Node.rightChildIndex(node.id)
        }
      } else {
        if (splitLeft) {
          predictNodeIndex(node.leftNode.get, binnedFeatures, bins, unorderedFeatures)
        } else {
          predictNodeIndex(node.rightNode.get, binnedFeatures, bins, unorderedFeatures)
        }
      }
    }
  }

  /**
   * Helper for binSeqOp, for data which can contain a mix of ordered and unordered features.
   *
   * For ordered features, a single bin is updated.
   * For unordered features, bins correspond to subsets of categories; either the left or right bin
   * for each subset is updated.
   *
   * @param agg  Array storing aggregate calculation, with a set of sufficient statistics for
   *             each (node, feature, bin).
   * @param treePoint  Data point being aggregated.
   * @param nodeIndex  Node corresponding to treePoint.  agg is indexed in [0, numNodes).
   * @param bins possible bins for all features, indexed (numFeatures)(numBins)
   * @param unorderedFeatures  Set of indices of unordered features.
   * @param instanceWeight  Weight (importance) of instance in dataset.
   */
  private def mixedBinSeqOp(
      agg: DTStatsAggregator,
      treePoint: TreePoint,
      nodeIndex: Int,
      bins: Array[Array[Bin]],
      unorderedFeatures: Set[Int],
      instanceWeight: Double,
      featuresForNode: Option[Array[Int]]): Unit = {
    val numFeaturesPerNode = if (featuresForNode.nonEmpty) {
      // Use subsampled features
      featuresForNode.get.size
    } else {
      // Use all features
      agg.metadata.numFeatures
    }
    val nodeOffset = agg.getNodeOffset(nodeIndex)
    // Iterate over features.
    var featureIndexIdx = 0
    while (featureIndexIdx < numFeaturesPerNode) {
      val featureIndex = if (featuresForNode.nonEmpty) {
        featuresForNode.get.apply(featureIndexIdx)
      } else {
        featureIndexIdx
      }
      if (unorderedFeatures.contains(featureIndex)) {
        // Unordered feature
        val featureValue = treePoint.binnedFeatures(featureIndex)
        val (leftNodeFeatureOffset, rightNodeFeatureOffset) =
          agg.getLeftRightNodeFeatureOffsets(nodeIndex, featureIndexIdx)
        // Update the left or right bin for each split.
        val numSplits = agg.metadata.numSplits(featureIndex)
        var splitIndex = 0
        while (splitIndex < numSplits) {
          if (bins(featureIndex)(splitIndex).highSplit.categories.contains(featureValue)) {
            agg.nodeFeatureUpdate(leftNodeFeatureOffset, splitIndex, treePoint.label,
              instanceWeight)
          } else {
            agg.nodeFeatureUpdate(rightNodeFeatureOffset, splitIndex, treePoint.label,
              instanceWeight)
          }
          splitIndex += 1
        }
      } else {
        // Ordered feature
        val binIndex = treePoint.binnedFeatures(featureIndex)
        agg.nodeUpdate(nodeOffset, nodeIndex, featureIndexIdx, binIndex, treePoint.label,
          instanceWeight)
      }
      featureIndexIdx += 1
    }
  }

  /**
   * Helper for binSeqOp, for regression and for classification with only ordered features.
   *
   * For each feature, the sufficient statistics of one bin are updated.
   *
   * @param agg  Array storing aggregate calculation, with a set of sufficient statistics for
   *             each (node, feature, bin).
   * @param treePoint  Data point being aggregated.
   * @param nodeIndex  Node corresponding to treePoint.  agg is indexed in [0, numNodes).
   * @param instanceWeight  Weight (importance) of instance in dataset.
   */
  private def orderedBinSeqOp(
      agg: DTStatsAggregator,
      treePoint: TreePoint,
      nodeIndex: Int,
      instanceWeight: Double,
      featuresForNode: Option[Array[Int]]): Unit = {
    val label = treePoint.label
    val nodeOffset = agg.getNodeOffset(nodeIndex)
    // Iterate over features.
    if (featuresForNode.nonEmpty) {
      // Use subsampled features
      var featureIndexIdx = 0
      while (featureIndexIdx < featuresForNode.get.size) {
        val binIndex = treePoint.binnedFeatures(featuresForNode.get.apply(featureIndexIdx))
        agg.nodeUpdate(nodeOffset, nodeIndex, featureIndexIdx, binIndex, label, instanceWeight)
        featureIndexIdx += 1
      }
    } else {
      // Use all features
      val numFeatures = agg.metadata.numFeatures
      var featureIndex = 0
      while (featureIndex < numFeatures) {
        val binIndex = treePoint.binnedFeatures(featureIndex)
        agg.nodeUpdate(nodeOffset, nodeIndex, featureIndex, binIndex, label, instanceWeight)
        featureIndex += 1
      }
    }
  }

  /**
   * Given a group of nodes, this finds the best split for each node.
   *
   * @param input Training data: RDD of [[org.apache.spark.mllib.tree.impl.TreePoint]]
   * @param metadata Learning and dataset metadata
   * @param topNodes Root node for each tree.  Used for matching instances with nodes.
   * @param nodesForGroup Mapping: treeIndex --> nodes to be split in tree
   * @param treeToNodeToIndexInfo Mapping: treeIndex --> nodeIndex --> nodeIndexInfo,
   *                              where nodeIndexInfo stores the index in the group and the
   *                              feature subsets (if using feature subsets).
   * @param splits possible splits for all features, indexed (numFeatures)(numSplits)
   * @param bins possible bins for all features, indexed (numFeatures)(numBins)
   * @param nodeQueue  Queue of nodes to split, with values (treeIndex, node).
   *                   Updated with new non-leaf nodes which are created.
   */
  private[tree] def findBestSplits(
      input: RDD[BaggedPoint[TreePoint]],
      metadata: DecisionTreeMetadata,
      topNodes: Array[Node],
      nodesForGroup: Map[Int, Array[Node]],
      treeToNodeToIndexInfo: Map[Int, Map[Int, NodeIndexInfo]],
      splits: Array[Array[Split]],
      bins: Array[Array[Bin]],
      nodeQueue: mutable.Queue[(Int, Node)],
      timer: TimeTracker = new TimeTracker): Unit = {

    /*
     * The high-level descriptions of the best split optimizations are noted here.
     *
     * *Group-wise training*
     * We perform bin calculations for groups of nodes to reduce the number of
     * passes over the data.  Each iteration requires more computation and storage,
     * but saves several iterations over the data.
     *
     * *Bin-wise computation*
     * We use a bin-wise best split computation strategy instead of a straightforward best split
     * computation strategy. Instead of analyzing each sample for contribution to the left/right
     * child node impurity of every split, we first categorize each feature of a sample into a
     * bin. We exploit this structure to calculate aggregates for bins and then use these aggregates
     * to calculate information gain for each split.
     *
     * *Aggregation over partitions*
     * Instead of performing a flatMap/reduceByKey operation, we exploit the fact that we know
     * the number of splits in advance. Thus, we store the aggregates (at the appropriate
     * indices) in a single array for all bins and rely upon the RDD aggregate method to
     * drastically reduce the communication overhead.
     */

    // numNodes:  Number of nodes in this group
    val numNodes = nodesForGroup.values.map(_.size).sum
    logDebug("numNodes = " + numNodes)
    logDebug("numFeatures = " + metadata.numFeatures)
    logDebug("numClasses = " + metadata.numClasses)
    logDebug("isMulticlass = " + metadata.isMulticlass)
    logDebug("isMulticlassWithCategoricalFeatures = " +
      metadata.isMulticlassWithCategoricalFeatures)

    /**
     * Performs a sequential aggregation over a partition.
     *
     * Each data point contributes to one node. For each feature,
     * the aggregate sufficient statistics are updated for the relevant bins.
     *
     * @param agg  Array storing aggregate calculation, with a set of sufficient statistics for
     *             each (node, feature, bin).
     * @param baggedPoint   Data point being aggregated.
     * @return  agg
     */
    def binSeqOp(
        agg: DTStatsAggregator,
        baggedPoint: BaggedPoint[TreePoint]): DTStatsAggregator = {
      treeToNodeToIndexInfo.foreach { case (treeIndex, nodeIndexToInfo) =>
        val nodeIndex = predictNodeIndex(topNodes(treeIndex), baggedPoint.datum.binnedFeatures,
          bins, metadata.unorderedFeatures)
        val nodeInfo = nodeIndexToInfo.getOrElse(nodeIndex, null)
        // If the example does not reach a node in this group, then nodeIndex = null.
        if (nodeInfo != null) {
          val aggNodeIndex = nodeInfo.nodeIndexInGroup
          val featuresForNode = nodeInfo.featureSubset
          val instanceWeight = baggedPoint.subsampleWeights(treeIndex)
          if (metadata.unorderedFeatures.isEmpty) {
            orderedBinSeqOp(agg, baggedPoint.datum, aggNodeIndex, instanceWeight, featuresForNode)
          } else {
            mixedBinSeqOp(agg, baggedPoint.datum, aggNodeIndex, bins, metadata.unorderedFeatures,
              instanceWeight, featuresForNode)
          }
        }
      }
      agg
    }

    // Calculate bin aggregates.
    timer.start("aggregation")
    val binAggregates: DTStatsAggregator = {
      val initAgg = if (metadata.subsamplingFeatures) {
        new DTStatsAggregatorSubsampledFeatures(metadata, treeToNodeToIndexInfo)
      } else {
        new DTStatsAggregatorFixedFeatures(metadata, numNodes)
      }
      input.treeAggregate(initAgg)(binSeqOp, DTStatsAggregator.binCombOp)
    }
    timer.stop("aggregation")

    // Calculate best splits for all nodes in the group
    timer.start("chooseSplits")

    // Iterate over all nodes in this group.
    nodesForGroup.foreach { case (treeIndex, nodesForTree) =>
      nodesForTree.foreach { node =>
        val nodeIndex = node.id
        val nodeInfo = treeToNodeToIndexInfo(treeIndex)(nodeIndex)
        val aggNodeIndex = nodeInfo.nodeIndexInGroup
        val featuresForNode = nodeInfo.featureSubset
        val (split: Split, stats: InformationGainStats, predict: Predict) =
          binsToBestSplit(binAggregates, aggNodeIndex, splits, featuresForNode)
        logDebug("best split = " + split)

        // Extract info for this node.  Create children if not leaf.
        val isLeaf = (stats.gain <= 0) || (Node.indexToLevel(nodeIndex) == metadata.maxDepth)
        assert(node.id == nodeIndex)
        node.predict = predict.predict
        node.isLeaf = isLeaf
        node.stats = Some(stats)
        logDebug("Node = " + node)

        if (!isLeaf) {
          node.split = Some(split)
          node.leftNode = Some(Node.emptyNode(Node.leftChildIndex(nodeIndex)))
          node.rightNode = Some(Node.emptyNode(Node.rightChildIndex(nodeIndex)))
          nodeQueue.enqueue((treeIndex, node.leftNode.get))
          nodeQueue.enqueue((treeIndex, node.rightNode.get))
          logDebug("leftChildIndex = " + node.leftNode.get.id +
            ", impurity = " + stats.leftImpurity)
          logDebug("rightChildIndex = " + node.rightNode.get.id +
            ", impurity = " + stats.rightImpurity)
        }
      }
    }
    timer.stop("chooseSplits")
  }

  /**
   * Calculate the information gain for a given (feature, split) based upon left/right aggregates.
   * @param leftImpurityCalculator left node aggregates for this (feature, split)
   * @param rightImpurityCalculator right node aggregate for this (feature, split)
   * @return information gain and statistics for split
   */
  private def calculateGainForSplit(
      leftImpurityCalculator: ImpurityCalculator,
      rightImpurityCalculator: ImpurityCalculator,
      metadata: DecisionTreeMetadata): InformationGainStats = {
    val leftCount = leftImpurityCalculator.count
    val rightCount = rightImpurityCalculator.count

    // If left child or right child doesn't satisfy minimum instances per node,
    // then this split is invalid, return invalid information gain stats.
    if ((leftCount < metadata.minInstancesPerNode) ||
        (rightCount < metadata.minInstancesPerNode)) {
      return InformationGainStats.invalidInformationGainStats
    }

    val totalCount = leftCount + rightCount

    val parentNodeAgg = leftImpurityCalculator.copy
    parentNodeAgg.add(rightImpurityCalculator)

    val impurity = parentNodeAgg.calculate()

    val leftImpurity = leftImpurityCalculator.calculate() // Note: This equals 0 if count = 0
    val rightImpurity = rightImpurityCalculator.calculate()

    val leftWeight = leftCount / totalCount.toDouble
    val rightWeight = rightCount / totalCount.toDouble

    val gain = impurity - leftWeight * leftImpurity - rightWeight * rightImpurity

    // if information gain doesn't satisfy minimum information gain,
    // then this split is invalid, return invalid information gain stats.
    if (gain < metadata.minInfoGain) {
      return InformationGainStats.invalidInformationGainStats
    }

    new InformationGainStats(gain, impurity, leftImpurity, rightImpurity)
  }

  /**
   * Calculate predict value for current node, given stats of any split.
   * Note that this function is called only once for each node.
   * @param leftImpurityCalculator left node aggregates for a split
   * @param rightImpurityCalculator right node aggregates for a split
   * @return predict value for current node
   */
  private def calculatePredict(
      leftImpurityCalculator: ImpurityCalculator,
      rightImpurityCalculator: ImpurityCalculator): Predict =  {
    val parentNodeAgg = leftImpurityCalculator.copy
    parentNodeAgg.add(rightImpurityCalculator)
    val predict = parentNodeAgg.predict
    val prob = parentNodeAgg.prob(predict)

    new Predict(predict, prob)
  }

  /**
   * Find the best split for a node.
   * @param binAggregates Bin statistics.
   * @param nodeIndex Index into aggregates for node to split in this group.
   * @return tuple for best split: (Split, information gain, prediction at node)
   */
  private def binsToBestSplit(
      binAggregates: DTStatsAggregator,
      nodeIndex: Int,
      splits: Array[Array[Split]],
      featuresForNode: Option[Array[Int]]): (Split, InformationGainStats, Predict) = {

    val metadata: DecisionTreeMetadata = binAggregates.metadata

    // calculate predict only once
    var predict: Option[Predict] = None

    // For each (feature, split), calculate the gain, and select the best (feature, split).
    val (bestSplit, bestSplitStats) = Range(0, metadata.numFeaturesPerNode).map { featureIndexIdx =>
      val featureIndex = if (featuresForNode.nonEmpty) {
        featuresForNode.get.apply(featureIndexIdx)
      } else {
        featureIndexIdx
      }
      val numSplits = metadata.numSplits(featureIndex)
      if (metadata.isContinuous(featureIndex)) {
        // Cumulative sum (scanLeft) of bin statistics.
        // Afterwards, binAggregates for a bin is the sum of aggregates for
        // that bin + all preceding bins.
        val nodeFeatureOffset = binAggregates.getNodeFeatureOffset(nodeIndex, featureIndexIdx)
        var splitIndex = 0
        while (splitIndex < numSplits) {
          binAggregates.mergeForNodeFeature(nodeFeatureOffset, splitIndex + 1, splitIndex)
          splitIndex += 1
        }
        // Find best split.
        val (bestFeatureSplitIndex, bestFeatureGainStats) =
          Range(0, numSplits).map { case splitIdx =>
            val leftChildStats = binAggregates.getImpurityCalculator(nodeFeatureOffset, splitIdx)
            val rightChildStats = binAggregates.getImpurityCalculator(nodeFeatureOffset, numSplits)
            rightChildStats.subtract(leftChildStats)
            predict = Some(predict.getOrElse(calculatePredict(leftChildStats, rightChildStats)))
            val gainStats = calculateGainForSplit(leftChildStats, rightChildStats, metadata)
            (splitIdx, gainStats)
          }.maxBy(_._2.gain)
        (splits(featureIndex)(bestFeatureSplitIndex), bestFeatureGainStats)
      } else if (metadata.isUnordered(featureIndex)) {
        // Unordered categorical feature
        val (leftChildOffset, rightChildOffset) =
          binAggregates.getLeftRightNodeFeatureOffsets(nodeIndex, featureIndexIdx)
        val (bestFeatureSplitIndex, bestFeatureGainStats) =
          Range(0, numSplits).map { splitIndex =>
            val leftChildStats = binAggregates.getImpurityCalculator(leftChildOffset, splitIndex)
            val rightChildStats = binAggregates.getImpurityCalculator(rightChildOffset, splitIndex)
            predict = Some(predict.getOrElse(calculatePredict(leftChildStats, rightChildStats)))
            val gainStats = calculateGainForSplit(leftChildStats, rightChildStats, metadata)
            (splitIndex, gainStats)
          }.maxBy(_._2.gain)
        (splits(featureIndex)(bestFeatureSplitIndex), bestFeatureGainStats)
      } else {
        // Ordered categorical feature
        val nodeFeatureOffset = binAggregates.getNodeFeatureOffset(nodeIndex, featureIndexIdx)
        val numBins = metadata.numBins(featureIndex)

        /* Each bin is one category (feature value).
         * The bins are ordered based on centroidForCategories, and this ordering determines which
         * splits are considered.  (With K categories, we consider K - 1 possible splits.)
         *
         * centroidForCategories is a list: (category, centroid)
         */
        val centroidForCategories = if (metadata.isMulticlass) {
          // For categorical variables in multiclass classification,
          // the bins are ordered by the impurity of their corresponding labels.
          Range(0, numBins).map { case featureValue =>
            val categoryStats = binAggregates.getImpurityCalculator(nodeFeatureOffset, featureValue)
            val centroid = if (categoryStats.count != 0) {
              categoryStats.calculate()
            } else {
              Double.MaxValue
            }
            (featureValue, centroid)
          }
        } else { // regression or binary classification
          // For categorical variables in regression and binary classification,
          // the bins are ordered by the centroid of their corresponding labels.
          Range(0, numBins).map { case featureValue =>
            val categoryStats = binAggregates.getImpurityCalculator(nodeFeatureOffset, featureValue)
            val centroid = if (categoryStats.count != 0) {
              categoryStats.predict
            } else {
              Double.MaxValue
            }
            (featureValue, centroid)
          }
        }

        logDebug("Centroids for categorical variable: " + centroidForCategories.mkString(","))

        // bins sorted by centroids
        val categoriesSortedByCentroid = centroidForCategories.toList.sortBy(_._2)

        logDebug("Sorted centroids for categorical variable = " +
          categoriesSortedByCentroid.mkString(","))

        // Cumulative sum (scanLeft) of bin statistics.
        // Afterwards, binAggregates for a bin is the sum of aggregates for
        // that bin + all preceding bins.
        var splitIndex = 0
        while (splitIndex < numSplits) {
          val currentCategory = categoriesSortedByCentroid(splitIndex)._1
          val nextCategory = categoriesSortedByCentroid(splitIndex + 1)._1
          binAggregates.mergeForNodeFeature(nodeFeatureOffset, nextCategory, currentCategory)
          splitIndex += 1
        }
        // lastCategory = index of bin with total aggregates for this (node, feature)
        val lastCategory = categoriesSortedByCentroid.last._1
        // Find best split.
        val (bestFeatureSplitIndex, bestFeatureGainStats) =
          Range(0, numSplits).map { splitIndex =>
            val featureValue = categoriesSortedByCentroid(splitIndex)._1
            val leftChildStats =
              binAggregates.getImpurityCalculator(nodeFeatureOffset, featureValue)
            val rightChildStats =
              binAggregates.getImpurityCalculator(nodeFeatureOffset, lastCategory)
            rightChildStats.subtract(leftChildStats)
            predict = Some(predict.getOrElse(calculatePredict(leftChildStats, rightChildStats)))
            val gainStats = calculateGainForSplit(leftChildStats, rightChildStats, metadata)
            (splitIndex, gainStats)
          }.maxBy(_._2.gain)
        val categoriesForSplit =
          categoriesSortedByCentroid.map(_._1.toDouble).slice(0, bestFeatureSplitIndex + 1)
        val bestFeatureSplit =
          new Split(featureIndex, Double.MinValue, Categorical, categoriesForSplit)
        (bestFeatureSplit, bestFeatureGainStats)
      }
    }.maxBy(_._2.gain)

    assert(predict.isDefined, "must calculate predict for each node")

    (bestSplit, bestSplitStats, predict.get)
  }

  /**
   * Returns splits and bins for decision tree calculation.
   * Continuous and categorical features are handled differently.
   *
   * Continuous features:
   *   For each feature, there are numBins - 1 possible splits representing the possible binary
   *   decisions at each node in the tree.
   *   This finds locations (feature values) for splits using a subsample of the data.
   *
   * Categorical features:
   *   For each feature, there is 1 bin per split.
   *   Splits and bins are handled in 2 ways:
   *   (a) "unordered features"
   *       For multiclass classification with a low-arity feature
   *       (i.e., if isMulticlass && isSpaceSufficientForAllCategoricalSplits),
   *       the feature is split based on subsets of categories.
   *   (b) "ordered features"
   *       For regression and binary classification,
   *       and for multiclass classification with a high-arity feature,
   *       there is one bin per category.
   *
   * @param input Training data: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]]
   * @param metadata Learning and dataset metadata
   * @return A tuple of (splits, bins).
   *         Splits is an Array of [[org.apache.spark.mllib.tree.model.Split]]
   *          of size (numFeatures, numSplits).
   *         Bins is an Array of [[org.apache.spark.mllib.tree.model.Bin]]
   *          of size (numFeatures, numBins).
   */
  protected[tree] def findSplitsBins(
      input: RDD[LabeledPoint],
      metadata: DecisionTreeMetadata): (Array[Array[Split]], Array[Array[Bin]]) = {

    logDebug("isMulticlass = " + metadata.isMulticlass)

    val numFeatures = metadata.numFeatures

    // Sample the input only if there are continuous features.
    val hasContinuousFeatures = Range(0, numFeatures).exists(metadata.isContinuous)
    val sampledInput = if (hasContinuousFeatures) {
      // Calculate the number of samples for approximate quantile calculation.
      val requiredSamples = math.max(metadata.maxBins * metadata.maxBins, 10000)
      val fraction = if (requiredSamples < metadata.numExamples) {
        requiredSamples.toDouble / metadata.numExamples
      } else {
        1.0
      }
      logDebug("fraction of data used for calculating quantiles = " + fraction)
      input.sample(withReplacement = false, fraction, new XORShiftRandom().nextInt()).collect()
    } else {
      new Array[LabeledPoint](0)
    }

    metadata.quantileStrategy match {
      case Sort =>
        val splits = new Array[Array[Split]](numFeatures)
        val bins = new Array[Array[Bin]](numFeatures)

        // Find all splits.
        // Iterate over all features.
        var featureIndex = 0
        while (featureIndex < numFeatures) {
          val numSplits = metadata.numSplits(featureIndex)
          val numBins = metadata.numBins(featureIndex)
          if (metadata.isContinuous(featureIndex)) {
            val numSamples = sampledInput.length
            splits(featureIndex) = new Array[Split](numSplits)
            bins(featureIndex) = new Array[Bin](numBins)
            val featureSamples = sampledInput.map(lp => lp.features(featureIndex)).sorted
            val stride: Double = numSamples.toDouble / metadata.numBins(featureIndex)
            logDebug("stride = " + stride)
            for (splitIndex <- 0 until numSplits) {
              val sampleIndex = splitIndex * stride.toInt
              // Set threshold halfway in between 2 samples.
              val threshold = (featureSamples(sampleIndex) + featureSamples(sampleIndex + 1)) / 2.0
              splits(featureIndex)(splitIndex) =
                new Split(featureIndex, threshold, Continuous, List())
            }
            bins(featureIndex)(0) = new Bin(new DummyLowSplit(featureIndex, Continuous),
              splits(featureIndex)(0), Continuous, Double.MinValue)
            for (splitIndex <- 1 until numSplits) {
              bins(featureIndex)(splitIndex) =
                new Bin(splits(featureIndex)(splitIndex - 1), splits(featureIndex)(splitIndex),
                  Continuous, Double.MinValue)
            }
            bins(featureIndex)(numSplits) = new Bin(splits(featureIndex)(numSplits - 1),
              new DummyHighSplit(featureIndex, Continuous), Continuous, Double.MinValue)
          } else {
            // Categorical feature
            val featureArity = metadata.featureArity(featureIndex)
            if (metadata.isUnordered(featureIndex)) {
              // TODO: The second half of the bins are unused.  Actually, we could just use
              //       splits and not build bins for unordered features.  That should be part of
              //       a later PR since it will require changing other code (using splits instead
              //       of bins in a few places).
              // Unordered features
              //   2^(maxFeatureValue - 1) - 1 combinations
              splits(featureIndex) = new Array[Split](numSplits)
              bins(featureIndex) = new Array[Bin](numBins)
              var splitIndex = 0
              while (splitIndex < numSplits) {
                val categories: List[Double] =
                  extractMultiClassCategories(splitIndex + 1, featureArity)
                splits(featureIndex)(splitIndex) =
                  new Split(featureIndex, Double.MinValue, Categorical, categories)
                bins(featureIndex)(splitIndex) = {
                  if (splitIndex == 0) {
                    new Bin(
                      new DummyCategoricalSplit(featureIndex, Categorical),
                      splits(featureIndex)(0),
                      Categorical,
                      Double.MinValue)
                  } else {
                    new Bin(
                      splits(featureIndex)(splitIndex - 1),
                      splits(featureIndex)(splitIndex),
                      Categorical,
                      Double.MinValue)
                  }
                }
                splitIndex += 1
              }
            } else {
              // Ordered features
              //   Bins correspond to feature values, so we do not need to compute splits or bins
              //   beforehand.  Splits are constructed as needed during training.
              splits(featureIndex) = new Array[Split](0)
              bins(featureIndex) = new Array[Bin](0)
            }
          }
          featureIndex += 1
        }
        (splits, bins)
      case MinMax =>
        throw new UnsupportedOperationException("minmax not supported yet.")
      case ApproxHist =>
        throw new UnsupportedOperationException("approximate histogram not supported yet.")
    }
  }

  /**
   * Nested method to extract list of eligible categories given an index. It extracts the
   * position of ones in a binary representation of the input. If binary
   * representation of an number is 01101 (13), the output list should (3.0, 2.0,
   * 0.0). The maxFeatureValue depict the number of rightmost digits that will be tested for ones.
   */
  private[tree] def extractMultiClassCategories(
      input: Int,
      maxFeatureValue: Int): List[Double] = {
    var categories = List[Double]()
    var j = 0
    var bitShiftedInput = input
    while (j < maxFeatureValue) {
      if (bitShiftedInput % 2 != 0) {
        // updating the list of categories.
        categories = j.toDouble :: categories
      }
      // Right shift by one
      bitShiftedInput = bitShiftedInput >> 1
      j += 1
    }
    categories
  }

}
