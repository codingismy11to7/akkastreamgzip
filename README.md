Akka Streams Compression.gzip
=============================

Compressing streams with `.via(Compression.gzip)` seems to be significantly
worse than using `GZIPOutputStream` (with `StreamConverters.fromOutputStream`).

I've tested this across the matrix of Scala 2.12.8/2.13.0 &
Akka 2.5.23/2.6.0-M3 (all results were similar).

Note: in the interest of not checking in a huge data file, I've checked in
sample data with different types of (json) documents and their statistical
probabilities of showing up in the real data set. I then generate a large dummy
data file from these. In the real world, there is far less data duplication (of
course), and the results are significantly more dramatic than is shown here.

Summary:

* `Compression.gzip` with level `9` takes about twice as long as level `5`
  * level `1` is faster than `GZIPOutputStream`, but is ~50% larger
* The time taken for `GZIPOutputStream` is about halfway between levels `1` & `5`
  with `Compression.gzip`
  * Its resulting file size is significantly smaller than even level `9` using 
    `Compression.gzip`
* Again, the file size differences are much more pronounced on real data

(As an aside, I tested `Compression.gunzip` vs `GZIPInputStream` on my real data,
and `Compression.gunzip` very slightly beat out `GZIS` in terms of speed on
every run, although probably not statistically significantly.)

### Sample Output (2018 Core i9 MacBook Pro on fast NVMe storage):
```
Generating input data
Wrote 1619.4 MB to /var/folders/33/ntdshx496qz9_qjq88tfd23r0000gn/T/sample.3255426563586355027.txt
************
** Run #1 **
************
* Writing file with Compression.gzip, level=fastest
Took 17 seconds
File is 146.6 MB

* Writing file with Compression.gzip, level=5
Took 23 seconds
File is 118.3 MB

* Writing file with Compression.gzip, level=best
Took 45 seconds
File is 115.2 MB

* Writing file with GZIPOutputStream
Took 20 seconds
File is 104.0 MB

************
** Run #2 **
************
* Writing file with Compression.gzip, level=fastest
Took 16 seconds
File is 146.6 MB

* Writing file with Compression.gzip, level=5
Took 22 seconds
File is 118.3 MB

* Writing file with Compression.gzip, level=best
Took 42 seconds
File is 115.2 MB

* Writing file with GZIPOutputStream
Took 20 seconds
File is 104.0 MB

************
** Run #3 **
************
* Writing file with Compression.gzip, level=fastest
Took 16 seconds
File is 146.6 MB

* Writing file with Compression.gzip, level=5
Took 22 seconds
File is 118.3 MB

* Writing file with Compression.gzip, level=best
Took 42 seconds
File is 115.2 MB

* Writing file with GZIPOutputStream
Took 20 seconds
File is 104.0 MB
```
