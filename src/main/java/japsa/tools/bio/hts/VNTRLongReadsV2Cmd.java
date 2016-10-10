/*****************************************************************************
 * Copyright (c) Minh Duc Cao, Monash Uni & UQ, All rights reserved.         *
 *                                                                           *
 * Redistribution and use in source and binary forms, with or without        *
 * modification, are permitted provided that the following conditions        *
 * are met:                                                                  * 
 *                                                                           *
 * 1. Redistributions of source code must retain the above copyright notice, *
 *    this list of conditions and the following disclaimer.                  *
 * 2. Redistributions in binary form must reproduce the above copyright      *
 *    notice, this list of conditions and the following disclaimer in the    *
 *    documentation and/or other materials provided with the distribution.   *
 * 3. Neither the names of the institutions nor the names of the contributors*
 *    may be used to endorse or promote products derived from this software  *
 *    without specific prior written permission.                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
 ****************************************************************************/

/****************************************************************************
 *                           Revision History                                
 * 10/10/2016 - Minh Duc Cao: forked from v1
 *  
 ****************************************************************************/
package japsa.tools.bio.hts;

import japsa.bio.alignment.ProfileDP;
import japsa.bio.alignment.ProfileDP.EmissionState;
import japsa.bio.tr.TandemRepeat;
import japsa.bio.tr.TandemRepeatVariant;
import japsa.seq.Alphabet;
import japsa.seq.SequenceOutputStream;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import japsa.seq.XAFReader;
import japsa.util.ByteArray;
import japsa.util.CommandLine;
import japsa.util.DoubleArray;
import japsa.util.IntArray;
import japsa.util.Logging;
import japsa.util.deploy.Deployable;

import java.io.File;
import java.util.HashMap;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;


/**
 * VNTR typing using long reads
 * 
 */

@Deployable(scriptName = "jsa.tr.longreadsv2", scriptDesc = "VNTR typing using long reads")
public class VNTRLongReadsV2Cmd  extends CommandLine {
	public VNTRLongReadsV2Cmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		addString("reference", null, "Name of the reference genome ", true);
		///addStdInputFile();
		addString("bamFile", null, "Name of the bam file", true);
		addString("output", "-",
				"Name of the output file, -  for stdout");
		addString("xafFile", null, "Name of the regions file in xaf",
				true);
		addInt("flanking", 30, "Size of the flanking regions");
		addInt("qual", 0, "Minimum quality");
		addInt("iteration", 1, "Number of iteration");
		addInt("nploidy",2,
				"The ploidy of the genome 1 =  happloid, 2 = diploid. Currenly only support up to 2-ploidy");
		addString("prefix", "",
				"Prefix of temporary files, if not specified, will be automatically generated");

		addStdHelp();		
	} 


	static Alphabet dna = Alphabet.DNA16();

	public static void main(String[] args) throws Exception,
	InterruptedException {
		/*********************** Setting up script ****************************/
		CommandLine cmdLine = new VNTRLongReadsV2Cmd();
		args = cmdLine.stdParseLine(args);
		/**********************************************************************/
		// Get options

		int flanking = cmdLine.getIntVal("flanking");
		if (flanking < 10)
			flanking = 10;

		int qual = cmdLine.getIntVal("qual");

		int np = cmdLine.getIntVal("nploidy");
		if (np > 2) {
			System.err.println("The program currenly only support haploid and diployd. Enter nploidy of 1 or 2");
			System.exit(1);
		}

		String bamFile = cmdLine.getStringVal("bamFile");
		String prefix = cmdLine.getStringVal("prefix");

		if (prefix == null || prefix.length() == 0) {
			prefix = "p" + System.currentTimeMillis();
		}
		/**********************************************************************/

		SequenceOutputStream outOS = SequenceOutputStream
				.makeOutputStream(cmdLine.getStringVal("output"));

		String[] headers = TandemRepeatVariant.SIMPLE_HEADERS;
		if (np > 1) {
			headers = TandemRepeatVariant.SIMPLE_HEADERS2;
		}

		TandemRepeatVariant.printHeader(outOS, headers);

		String strFile = cmdLine.getStringVal("xafFile");

		Logging.info("Read genome begins");
		HashMap <String, Sequence> genome = new HashMap <String, Sequence>();
		SequenceReader seqReader = SequenceReader.getReader(cmdLine.getStringVal("reference"));
		Sequence seq;
		while ((seq = seqReader.nextSequence(dna)) != null){
			genome.put(seq.getName(), seq);
		}
		seqReader.close();
		Logging.info("Read genome done");

		/**********************************************************************/
		XAFReader xafReader = new XAFReader(strFile);

		SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
		SamReader reader = SamReaderFactory.makeDefault().open(new File(bamFile));						

		IntArray intArray = new IntArray();
		DoubleArray doubleArray = new DoubleArray();
		ByteArray byteArray = new ByteArray(); 

		//int _tIndex = 0;
		while (xafReader.next() != null){	
			//_tIndex ++;
			TandemRepeat str = TandemRepeat.read(xafReader);

			int start = Integer.parseInt(xafReader.getField("start")) - flanking;			
			int end = Integer.parseInt(xafReader.getField("end")) + flanking;
			String chrom = xafReader.getField("chrom");					

			if (seq == null || !seq.getName().equals(chrom)){
				seq = genome.get(chrom);
			}
			if (seq == null){
				xafReader.close();
				//sos.close();
				Logging.exit("Chrom in line " + xafReader.lineNo() + " not found!!!", 1);
			}

			if (end > seq.length())
				end = seq.length();

			if (start < 1)
				start = 1;

			int hmmFlank = flanking;
			int hmmPad = 
					(int)((str.getUnitNo() - Math.floor(str.getUnitNo())) * str.getPeriod()) ;

			//System.out.println("###" + str.getPeriod() + " " + str.getUnitNo() + "   " + hmmPad);			
			Sequence hmmSeq = new Sequence(dna, hmmFlank * 2 + hmmPad + str.getPeriod());
			int i = 0;

			for (;i < hmmFlank + hmmPad + str.getPeriod(); i++)
				hmmSeq.setBase(i, seq.getBase(str.getStart() - hmmFlank + i -1));

			for (;i < hmmSeq.length();i++){
				byte base = seq.getBase(str.getEnd() + i - (hmmFlank + hmmPad + str.getPeriod()) );//no need to -1
				hmmSeq.setBase(i,base);				
			}

			ProfileDP dp = new ProfileDP(hmmSeq, hmmFlank + hmmPad, hmmFlank + hmmPad + str.getPeriod() - 1);//-1 for 0-index, inclusive

			//System.out.println("Lengths: " + hmmFlank + ", " + hmmPad + " " + str.getPeriod() + " " + hmmSeq.length() );			
			//System.out.println("CHECKING BEGIN");

			outOS.print("##"+str.getID()+"\n## ");
			for (int x = 0; x < hmmSeq.length();x++){
				outOS.print(hmmSeq.charAt(x));
				if (x == hmmFlank + hmmPad -1 || x ==  hmmFlank + hmmPad + str.getPeriod() - 1)
					outOS.print("==");
			}
			outOS.println();

			Sequence refRepeat = seq.subSequence(start, end);
			refRepeat.setName("reference");

			//run on the reference
			//if (1==0)
			{
				EmissionState bestState = dp.align(refRepeat);
				double alignScore = bestState.getScore();
				//System.out.println("Score " + alignScore + " vs " + readSeq.length()*2 + " (" + alignScore/readSeq.length() +")");
				int bestIter = bestState.getIter();
				outOS.print("##" + refRepeat.getName()+"\t"+bestIter+"\t"+refRepeat.length() +"\t" +alignScore+"\t" + alignScore/refRepeat.length() + '\n');


				/*********************************************************/
				intArray.clear();
				doubleArray.clear();
				byteArray.clear();				

				//double oldCost = bestState.score;
				EmissionState lastState = bestState;				
				bestState = bestState.bwdState;

				while (bestState != null){
					intArray.add(bestState.profilePos);						
					doubleArray.add(lastState.score - bestState.score);

					if (bestState.seqPos == lastState.seqPos)
						byteArray.add((byte)Alphabet.DNA.GAP);
					else
						byteArray.add(refRepeat.getBase(lastState.seqPos));						

					lastState = bestState;
					bestState = bestState.bwdState;
				}					

				double costL = 0, costR = 0;

				for (int x = intArray.size() - 1; x >=0; x--){
					outOS.print(Alphabet.DNA().int2char(byteArray.get(x)));

					//
					if (x <intArray.size() - 1  && intArray.get(x) <  intArray.get(x+1)){
						outOS.println();	 
					}

					if (intArray.get(x) < hmmFlank + hmmPad)
						costL += doubleArray.get(x);
					if (intArray.get(x) > hmmFlank + hmmPad + str.getPeriod())
						costR += doubleArray.get(x);													
				}
				outOS.println();
				outOS.print ("L = " + (costL/(hmmFlank + hmmPad)) + " R = " + costR/(hmmSeq.length() - hmmFlank - hmmPad - str.getPeriod()) + "\n");
				outOS.print("==================================================================\n");
				/*********************************************************/
			}

			SAMRecordIterator iter = reader.query(str.getParent(), start, end, false);

			String fileName = prefix + "_" + str.getID() + "_i.fasta";
			SequenceOutputStream os = SequenceOutputStream.makeOutputStream(fileName);


			double var = 0;
			TandemRepeatVariant trVar = new TandemRepeatVariant();
			trVar.setTandemRepeat(str);

			int readIndex = 0;

			while (iter.hasNext()) {
				SAMRecord rec = iter.next();
				// Check qualilty
				if (rec.getMappingQuality() < qual) {
					continue;
				}

				// Only reads that fully span the repeat and flankings
				int currentRefPos = rec.getAlignmentStart();
				if (currentRefPos > start)
					continue;
				if (rec.getAlignmentEnd() < end)
					continue;

				readIndex ++;
				////////////////////////////////////////////////////////////////////
				//assert currentRefBase < start

				Sequence readSeq = getReadPosition(rec,start,end);
				if (readSeq == null)
					continue;

				readSeq.writeFasta(os);

				EmissionState bestState = dp.align(readSeq);
				double alignScore = bestState.getScore();
				//System.out.println("Score " + alignScore + " vs " + readSeq.length()*2 + " (" + alignScore/readSeq.length() +")");
				int bestIter = bestState.getIter();

				/*******************************************************************/				
				intArray.clear();
				doubleArray.clear();
				byteArray.clear();				

				//double oldCost = bestState.score;
				EmissionState lastState = bestState;				
				bestState = bestState.bwdState;

				while (bestState != null){
					intArray.add(bestState.profilePos);						
					doubleArray.add(lastState.score - bestState.score);

					if (bestState.seqPos == lastState.seqPos)
						byteArray.add((byte)Alphabet.DNA.GAP);
					else
						byteArray.add(readSeq.getBase(lastState.seqPos));						

					lastState = bestState;
					bestState = bestState.bwdState;
				}					

				double costL = 0, costR = 0;

				for (int x = intArray.size() - 1; x >=0; x--){
					outOS.print(Alphabet.DNA().int2char(byteArray.get(x)));

					//end of a repeat cycle
					if (x <intArray.size() - 1  && intArray.get(x) < intArray.get(x+1)){
						outOS.println();	 
					}

					//left 
					if (x <intArray.size() - 1  && intArray.get(x) < hmmFlank + hmmPad && intArray.get(x + 1) >= hmmFlank + hmmPad)
						outOS.println();

					//right
					if (x <intArray.size() - 1  && intArray.get(x) < hmmFlank + hmmPad + str.getPeriod() && intArray.get(x + 1) >= hmmFlank + hmmPad + str.getPeriod())
						outOS.println();


					if (intArray.get(x) < hmmFlank + hmmPad)
						costL += doubleArray.get(x);
					if (intArray.get(x) > hmmFlank + hmmPad + str.getPeriod())
						costR += doubleArray.get(x);													
				}
				outOS.println();
				outOS.print ("L = " + (costL/(hmmFlank + hmmPad)) + " R = " + costR/(hmmSeq.length() - hmmFlank - hmmPad - str.getPeriod()) + "\n");				


				String readName = readSeq.getName();
				String [] toks =  readName.split("/",4);

				//String polymerageRead = toks[0] + "/" + toks[1];

				String polymerageRead = (toks.length > 1)?toks[1]:toks[0];
				String subRead = (toks.length > 2)?toks[2]:"_";
				String alignSubRead = (toks.length > 3)?toks[3]:"_";

				/*****************************************************************/				
				outOS.print("##" + polymerageRead + "_" + subRead +"\t"+bestIter+"\t"+readSeq.length() +"\t" +alignScore+"\t" + alignScore/readSeq.length() + '\t' + readSeq.getDesc() + '\n');
				outOS.print("==================================================================\n");				
			}// while
			iter.close();
			os.close();

			outOS.print(trVar.toString(headers));
			outOS.print('\n');
		}// for

		reader.close();
		outOS.close();
	}

	public static Sequence getReadPosition(SAMRecord rec, int startRef, int endRef){
		byte[]  seqRead = rec.getReadBases();//
		if (seqRead.length <= 1)
			return null;

		int startRead = -1, endRead = -1;

		int refPos = rec.getAlignmentStart();
		int readPos = 0;		
		//currentRefPos <= startRead				

		for (final CigarElement e : rec.getCigar().getCigarElements()) {
			int length = e.getLength();
			switch (e.getOperator()) {
			case H:
				break; // ignore hard clips
			case P:
				break; // ignore pads
			case S:
				readPos += e.getLength();								
				break; // soft clip read bases
			case N: // N ~ D
			case D:
				refPos += length;

				if (startRead < 0  && refPos >= startRef){					
					startRead = readPos;
				}

				if (endRead < 0  && refPos >= endRef){					
					endRead = readPos;
				}

				break;// case
			case I:				
				readPos += length;						
				break;

			case M:
			case EQ:
			case X:				
				if ((startRead < 0) && refPos + length >= startRef) {
					startRead = readPos + startRef - refPos;					
				}

				if ((endRead < 0) && (refPos + length >= endRef)){
					endRead = readPos + endRef - refPos;
				}

				refPos += length;
				readPos += length;				
				break;
			default:
				throw new IllegalStateException(
						"Case statement didn't deal with cigar op: "
								+ e.getOperator());
			}// case
			if (refPos >= endRef)
				break;//for

		}// for
		if (startRead < 0 || endRead < 0){
			Logging.warn(" " + refPos + "  " + readPos + " " + startRead + " " + endRead);
			return null;
		}		

		Alphabet alphabet = Alphabet.DNA16();
		Sequence retSeq = new Sequence(alphabet, endRead - startRead + 1, rec.getReadName() + "/" + startRead + "_" + endRead);
		for (int i = 0; i < retSeq.length();i++){
			retSeq.setBase(i, alphabet.byte2index(seqRead[startRead + i]));			
		}
		return retSeq;

	}
}
