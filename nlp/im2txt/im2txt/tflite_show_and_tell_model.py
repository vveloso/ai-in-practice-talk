import tensorflow as tf

from ops import image_processing

class TfliteShowAndTellModel(object):

  def __init__(self, imagenet_path, ltsm_path):
    self.imagenet = tf.lite.Interpreter(imagenet_path)
    self.imagenet.allocate_tensors()
    self.ltsm = tf.lite.Interpreter(ltsm_path)

  def input_index(self, model, name):
    return next(i for i in model.get_input_details() if i['name'] == name)['index']

  def output_index(self, model, name):
    return next(o for o in model.get_output_details() if o['name'] == name)['index']

  def feed_image(self, session, encoded_image):
    image = image_processing.process_image(encoded_image,
                                          is_training=False,
                                          height=299,
                                          width=299,
                                          resize_height=299,
                                          resize_width=299)
    self.imagenet.set_tensor(
        self.input_index(self.imagenet, 'ExpandDims_3'), tf.expand_dims(image, 0))
    self.imagenet.invoke()
    return self.imagenet.get_tensor(
        self.output_index(self.imagenet, 'lstm/initial_state'))

  def inference_step(self, session, input_feed, state_feed):
    self.ltsm.resize_tensor_input(
      self.input_index(self.ltsm, 'input_feed'), input_feed.shape)
    self.ltsm.resize_tensor_input(
      self.input_index(self.ltsm, 'lstm/state_feed'), state_feed.shape)
    self.ltsm.allocate_tensors()

    self.ltsm.set_tensor(
        self.input_index(self.ltsm, 'input_feed'), input_feed)
    self.ltsm.set_tensor(
        self.input_index(self.ltsm, 'lstm/state_feed'), state_feed)
    self.ltsm.invoke()
    softmax = self.ltsm.get_tensor(
        self.output_index(self.ltsm, 'softmax'))
    new_states = self.ltsm.get_tensor(
        self.output_index(self.ltsm, 'lstm/state'))
    return (softmax, new_states, None)
