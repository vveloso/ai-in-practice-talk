r"Converts a saved graph into the two TensorFlow Lite models necessary to run im2txt inference."

import os
import tensorflow.compat.v1 as tf

FLAGS = tf.flags.FLAGS

tf.flags.DEFINE_string("tflite_path", "model",
                       "Directory where the TFLite models are saved.")
tf.flags.DEFINE_string("input_graph", "model/frozen_graph.pb",
                       "File name for the frozen graph data.")

tf.logging.set_verbosity(tf.logging.INFO)

def build_converter(input_tensors, output_tensors):
     return tf.lite.TFLiteConverter.from_frozen_graph(
        FLAGS.input_graph,
        input_tensors,
        output_tensors
    )

def save_conversion(converter, file_name):
    model = converter.convert()
    with open(os.path.join(FLAGS.tflite_path, file_name), 'wb') as f:
        f.write(model)

imagenet_converter = build_converter(
    [ 'ExpandDims_3' ], [ 'lstm/initial_state' ])
imagenet_converter.optimizations = [ tf.lite.Optimize.DEFAULT ]
imagenet_converter.target_spec.supported_types = [tf.float16]
save_conversion(imagenet_converter, 'imagenet.tflite')

lstm_converter = build_converter(
    [ 'input_feed', 'lstm/state_feed' ], 
    [ 'softmax', 'lstm/state' ])
lstm_converter.optimizations = [ tf.lite.Optimize.DEFAULT ]
save_conversion(lstm_converter, 'ltsm.tflite')
