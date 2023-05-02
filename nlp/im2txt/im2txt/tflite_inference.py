import tensorflow.compat.v1 as tf

import math
import os

from inference_utils.vocabulary import Vocabulary 
from inference_utils.caption_generator import CaptionGenerator
from tflite_show_and_tell_model import TfliteShowAndTellModel

FLAGS = tf.flags.FLAGS

tf.flags.DEFINE_string("vocab_file", "data/Hugh/word_counts.txt", "Text file containing the vocabulary.")
tf.flags.DEFINE_string("input_file", "data/images/test.jpg", "Image file name.")
tf.flags.DEFINE_string("imagenet_file", "model/imagenet.tflite", "Image file name.")
tf.flags.DEFINE_string("ltsm_file", "model/ltsm.tflite", "Image file name.")

tf.logging.set_verbosity(tf.logging.INFO)

def main(_):

  # Load our model.
  model = TfliteShowAndTellModel(FLAGS.imagenet_file, FLAGS.ltsm_file)

  # Create the vocabulary.
  vocab = Vocabulary(FLAGS.vocab_file)

  # Prepare the caption generator.
  generator = CaptionGenerator(model, vocab)

  with tf.gfile.GFile(FLAGS.input_file, "rb") as f:
    image = f.read()
  captions = generator.beam_search(None, image)
  print("Captions for image %s:" % os.path.basename(FLAGS.input_file))
  for i, caption in enumerate(captions):
    # Ignore begin and end words.
    sentence = [vocab.id_to_word(w) for w in caption.sentence[1:-1]]
    sentence = " ".join(sentence)
    print("  %d) %s (p=%f)" % (i, sentence, math.exp(caption.logprob)))

if __name__ == "__main__":
  tf.app.run()
