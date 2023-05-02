import tensorflow.compat.v1 as tf

FLAGS = tf.flags.FLAGS

tf.flags.DEFINE_string("input_graph", "model/frozen_graph.pb",
                       "File name for the frozen graph data.")

tf.flags.DEFINE_string("logs_dir", "model/Hugh/logs",
                       "Output logs directory.")

with tf.Session() as sess:
    with tf.io.gfile.GFile(FLAGS.input_graph, 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        g_in = tf.import_graph_def(graph_def)

    train_writer = tf.summary.FileWriter(FLAGS.logs_dir)
    train_writer.add_graph(sess.graph)

print('You can now run tensorboard --logdir model/Hugh/logs')
