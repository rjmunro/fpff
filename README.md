FPFF
====

Audio fingerprinting utility version 0.0.2
Copyright (C) 2007 - 2014 Juha Hejoranta, Robert (Jamie) Munro

This utility is licensed under GPLv2 or (at your option) any later version.
See COPYING file for details.


Basic work flow how this utility works
--------------------------------------

This utility follows quite closely procedure described in
http://wiki.musicbrainz.org/FutureProofFingerPrintFunction

The utility reads 16 bit 8192 Hz signed audio data (little endian) from stdin
and prints audio fingerprint in intervals of 512 sample (62.5ms) to stdout.

Nearly all parameters can be tweaked by editing the fppff.properties file.

Below is a rough description of the procedure and some notes.


Requirements
------------
Java Runtime Environment 5.0 or any later
http://java.sun.com/javase/downloads/index.jsp

SoX - Sound eXchange, or any similar tool which can covert audio
http://sox.sourceforge.net/


Usage
-----

Usage: org.foo.Fpff [mode]

    Mode:
        --help          Show this message
        --raw           Print raw spectrum data
        --barks         Print spectrum data in barks
        --logarithmic   Print barks in adjusted logarithmic scale
        --symbol        Print symbol data (default)

The raw mode will print all power spectrum components as returned by fft()

The barks mode will print all barks mutiplied with decorrelation factors

The logarithmic mode prints barks in logarithmic scale adjusted by mean
decorrelation value. This will cause (theorically average) sample to each
bark value of 1.0. This all assuming that the decorrelation factors are
correct.

The symbol mode will print one of the base64 characters presenting the nearest
codebook entry of sample.


Example
-------

The sample.wav is 3s clip.

The $ presents the command prompt.

    $ sox sample.wav -t raw -r 8192 -s -w -c 1 sample.dat avg resample -ql
    $ java -jar dist/lib/Fpff-20070520.jar < sample.dat
    LLLfLuuuLLoDDEEELLkkkfLLLLDDDDDLLfcdkfffXuum0www


Tested in Linux. Should work in any platform which as sox and Java available.


Transform and filter audio data with sox
----------------------------------------

    sox audio.wav -t raw -r 8192 -s -w -c 1 audio.dat avg resample -ql

 * Write data in raw 16bit (little endian) format.
 * Average stereo channels to single channel
 * Resample to 8192 Hz


Feed data to fingerprint utility
--------------------------------

Read 4096 samples (500ms)

Get the power spectrum
 * Remove DC component
 * Do gain control (scale sample data)
 * Multiply with Hann window
 * Take fft()
 * Return power spectrum of fft()

Group the spectrum components to barks
 * Divide spectrum components to 16 barks
 * Calculate mean of each bark
 * Multiply each bark with corresponding decorrelation factor and return the
   values

Scale barks to logarithmically
 * Return log10() of the barks

Print the symbol
 * Select closet vector from codebook
 * Print the symbol presenting the codebook vector

Repeat with 512 sample intervals (62.5ms)

Thus, we have symbol rate of 8192 / 4096 = 16.


Notes
-----

* Sample window of 500ms combined with symbol rate of 16 seems good. It might
  be possible to increase sample window in order to decrease symbol rate.
* The current weak point is the codebook. Coming up with a good codebook is
  is *very* difficult. See below.
* Using 8 barks might work as current 16 barks.
  E.g. the firts bark: [100, 300).
  This would yield a small speed improvement with (hopefully) no impact to
  quality.


The codebook problem
--------------------

Creating of a good codebook is very difficult. Clustering multidimensional data
is difficult if it has to meet two requirements:

a) The cluster sizes are nearly equal.

b) The cluster centers are as far as possible from each other.

The requirements are relatively easy to fullfill by them selves. However, If
both must hold then the problem becomes way more difficult to solve.

It seems that there are some academic papers describing solution to this kind of
problem but software implementation available.

By generating *lot* of cluster sets satisfying criteria a) and then picking up
the best cluster set satisfying criteria b) we might end up with good enough
solution.

Why the b) requirement is important? Well, you can splice some object to equally
sized volumens by single axis, say x. Now, if this works then why to have other
axis at all? When the cluster centers are as far as they can from each other
then, in general, the small changes in single frequency component cannot change
the cluster where the sample belongs.


Current status
--------------

The current code is intended to be very generic from language point of view. It
should be relatively easy to port it to C, Python, etc.

It is not optimized in any way. This is all fully intentional at this stage of
developement.

Step 1. Get it working. Check.
Step 2. Get it right. 95 % Completed.
Step 3: Get it fast. To Be Done.

The current major task is to get the codebook correct. The other values like
decorrelation factors need minor adjustments.

Step 3 requires porting of the code to more suitable language like C.


Changes to 0.0.2 version
------------------------

* Some bug fixes
* New codebook and decorrelation factors
* Removed bandreject filter from sox arguments
