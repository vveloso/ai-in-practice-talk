# Copyright 2021 Vasco Veloso. Derived from original work that is 
# Copyright 2016 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
r"""Freeze the model."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function


import tensorflow.compat.v1 as tf

import configuration
import inference_wrapper

FLAGS = tf.flags.FLAGS

tf.flags.DEFINE_string("checkpoint_path", "model/Hugh/train/newmodel.ckpt-2000000",
                       "Model checkpoint file or directory containing a "
                       "model checkpoint file.")
tf.flags.DEFINE_string("output_graph", "model/frozen_graph.pb",
                       "File name for the frozen graph data.")

tf.logging.set_verbosity(tf.logging.INFO)


def main(_):

  # Build the inference graph.
  g = tf.Graph()
  with g.as_default():
    model = inference_wrapper.InferenceWrapper()
    restore_fn = model.build_graph_from_config(configuration.ModelConfig(),
                                               FLAGS.checkpoint_path)
  g.finalize()

  with tf.Session(graph=g) as sess:
    # Load the model from checkpoint.
    restore_fn(sess)

    frozen_graph_def = tf.graph_util.convert_variables_to_constants(
      sess, sess.graph_def,
      ["lstm/initial_state", "softmax", "lstm/state"])

    # Save the frozen graph.
    with open(FLAGS.output_graph, 'wb') as f:
      f.write(frozen_graph_def.SerializeToString())


if __name__ == "__main__":
  tf.app.run()
