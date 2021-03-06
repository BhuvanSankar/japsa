---------------------------------------------------------------------------------------
*npScaffolder*: real-time scaffolder using SPAdes contigs and Nanopore sequencing reads
---------------------------------------------------------------------------------------

*npScaffolder* (jsa.np.npscarf) is a program that connect contigs from a draft genomes 
to generate sequences that are closer to finish. These pipelines can run on a single laptop
for microbial datasets. In real-time mode, it can be integrated with simple structural 
analyses such as gene ordering, plasmid forming.

npScaffolder is included in the `Japsa package <http://mdcao.github.io/japsa/>`_.

~~~~~~~~
Synopsis
~~~~~~~~

*jsa.np.npscarf*: Scaffold and finish assemblies using Oxford Nanopore sequencing reads

~~~~~
Usage
~~~~~
::

   jsa.np.npscarf [options]

~~~~~~~
Options
~~~~~~~
  --seqFile=s     Name of the assembly file (sorted by length)
                  (REQUIRED)
  --bamFile=s     Name of the bam file
                  (REQUIRED)
  --spadesDir=s   Name of the output folder by SPAdes (on development)
                  (default='null')
  --prefix=s      Prefix for the output files
                  (default='out')
  --genes=s       Realtime annotation: name of annotated genes in GFF 3.0 format
                  (default='null')
  --resistGene=s  Realtime annotation: name of antibiotic resistance gene fasta file
                  (default='null')
  --insertSeq=s   Realtime annotation: name of IS fasta file
                  (default='null')
  --oriRep=s      Realtime annotation: name of fasta file containing possible origin of replication
                  (default='null')
  --minContig=i   Minimum contigs length that are used in scaffolding.
                  (default='300')
  --maxRepeat=i   Maximum length of repeat in considering species.
                  (default='7500')
  --cov=d         Expected average coverage of Illumina, <=0 to estimate
                  (default='0.0')
  --qual=i        Minimum quality
                  (default='1')
  --support=i     Minimum supporting long read needed for a link between markers
                  (default='1')
  --realtime      Process in real-time mode. Default is batch mode (false)
                  (default='false')
  --read=i        Minimum number of reads between analyses
                  (default='500')
  --time=i        Minimum number of seconds between analyses
                  (default='300')
  --verbose       Turn on debugging mode
                  (default='false')
  --help          Display this usage and exit
                  (default='false')


~~~~~~~~
See also
~~~~~~~~

jsa.np.npreader_, jsa.util.streamServer_, jsa.util.streamClient_

.. _jsa.np.npreader: jsa.np.npreader.html
.. _jsa.util.streamServer: jsa.util.streamServer.html
.. _jsa.util.streamClient: jsa.util.streamClient.html



~~~~~~~~~~~~~~
Usage examples
~~~~~~~~~~~~~~

A summary of *npScarf* usage can be obtained by invoking the --help option::

    jsa.np.npscarf --help
    
Input
=====
*npScarf* takes two files as required input::

	jsa.np.npscarf -s <*draft*> -b <*bam*>
	
<*draft*> input is the FASTA file containing the pre-assemblies. Normally this 
is the output from running SPAdes on Illumina MiSeq paired end reads.

<*bam*> contains SAM/BAM formated alignments between <*draft*> file and <*nanopore*> 
FASTA/FASTQ file of long read data. We use BWA-MEM as the recommended aligner 
with the fixed parameter set as follow::

	bwa mem -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -a -Y <*draft*> <*nanopore*> > <*bam*>
	
Output
=======
*npScarf* output is specified by *-prefix* option. The default prefix is \'out\'.
Normally the tool generate two files: *prefix*.fin.fasta and *prefix*.fin.japsa which 
indicate the result scaffolders in FASTA and JAPSA format.

In realtime mode, if any annotation analysis is enabled, a file named 
*prefix*.anno.japsa is generated instead. This file contains features detected after
scaffolding.

Real-time scaffolding
=====================
To run *npScarf* in streaming mode::

   	jsa.np.npscarf -realtime [options]

In this mode, the <*bam*> file will be processed block by block. The size of block 
(number of BAM/SAM records) can be manipulated through option *-read* and *-time*.

The idea of streaming mode is when the input <*nanopore*> file is retrieved in stream.
npReader is the module that provides such data from fast5 files returned from the real-time
base-calling cloud service Metrichor. Ones can run::

    jsa.np.npreader -realtime -folder c:\Downloads\ -fail -output - | \
      bwa mem -t 10 -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -a -Y -K 3000 <*draft*> - 2> /dev/null | \ 
      jsa.np.npscarf --realtime -b - -seq <*draft*> > log.out 2>&1

or if you have the whole set of Nanopore long reads already and want to emulate the 
streaming mode::

    jsa.np.timeEmulate -s 100 -i <*nanopore*> -output - | \
      bwa mem -t 10 -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -a -Y -K 3000 <*draft*> - 2> /dev/null | \ 
      jsa.np.npscarf --realtime -b - -seq <*draft*> > log.out 2>&1

Note that jsa.np.timeEmulate based on the field *timestamp* located in the read name line to
decide the order of streaming data. So if your input <*nanopore*> already contains the field,
you have to sort it::

    jsa.seq.sort -i <*nanopore*> -o <*nanopore-sorted*> -sortKey=timestamp

or if your file does not have the *timestamp* data yet, you can manually make ones. For example::

    cat <*nanopore*> | \
       awk 'BEGIN{time=0.0}NR%4==1{printf "%s timestamp=%.2f\n", $0, time; time++}NR%4!=1{print}' \
       > <*nanopore-with-time*> 

Real-time annotation
====================
The tool includes usecase for streaming annotation. Ones can provides database of antibiotic
resistance genes and/or Origin of Replication in FASTA format for the analysis of gene ordering
and/or plasmid identifying respectively::

    jsa.np.timeEmulate -s 100 -i <*nanopore*> -output - | \
      bwa mem -t 10 -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -a -Y -K 3000 <*draft*> - 2> /dev/null | \ 
      jsa.np.npscarf --realtime -b - -seq <*draft*> -resistGene <*resistDB*> -oriRep <*origDB*> > log.out 2>&1

Assembly graph
==============
*npScarf* can read the assembly graph info from SPAdes to make the results more precise down to SNP level.
This function is still on development and the results might be slightly deviate from the stable version in
term of number of final contigs::

    jsa.np.npscarf --spadesFolder=<SPAdes_output_directory> <options...>

where SPAdes_output_directory indicates the result folder of SPAdes, containing files such as contigs.fasta, 
contigs.paths and assembly_graph.fastg.
