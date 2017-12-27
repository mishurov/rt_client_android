# An android client for [the pathtracer](https://github.com/mishurov/rt_pathtracer)

The cilent sends transform, lens and other data over UDP to the render server and recieves a rendered JPEG encoded image stream.

An Android device has to have an accelerometer, a compass and magnitometer to determine the orientation of a device in a physical world.

Double tap to start Setting Activity, set up server's address: an IP or a hostname and the view mode: mono, stereo or lens distorted stereo.

The lens distortion is applied entirely on the client via Google VR SDK therefore it isn't the true raytraced lens distortion.

Some parameters for the stereo camera in the renderer are hand tuned, the app is tested on a Xiaomi Redmi Note 4X with a couple of headsets. I can't be sure that it will display the image stream correctly on every device.

