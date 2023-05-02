# Conversion to TensorFlow Lite

## Preparation

The project needs to be prepared as described in [the main README file](README.md), namely by downloading the [checkpoint files](https://drive.google.com/open?id=1r4-9FEIbOUyBSvA-fFVFgvhFpgee6sF5) and placing them in the `im2txt/model/Hugh/train` directory.

## A. Using TensorFlow 1.15

1. Test the model by running inference:

```
python im2txt/run_inference.py --checkpoint_path="im2txt/model/Hugh/train/newmodel.ckpt-2000000" --vocab_file="im2txt/data/Hugh/word_counts.txt" --input_files="im2txt/data/images/test.jpg"
```

2. Freeze the model graph:

```
python im2txt/freeze_model.py --checkpoint_path="im2txt/model/Hugh/train/newmodel.ckpt-2000000" --output_graph="im2txt/model/frozen_graph"
```

3. If you want to look at the graph in Tensorboard, prepare its log files:

```
python im2txt/to_tensorboard.py
```

## B. Using TensorFlow 2

1. Convert to TensorFlow Lite format:

```
python im2txt/tflite_convert.py
```

2. Test the TensorFlow Lite model by running inference with it:

```
python im2txt/tflite_inference.py
```
