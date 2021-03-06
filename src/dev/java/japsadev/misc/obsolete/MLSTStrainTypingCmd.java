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

/*****************************************************************************
 *                           Revision History                                
 * 7 Aug 2015 - Minh Duc Cao: Created                                        
 * 
 ****************************************************************************/
package japsadev.misc.obsolete;

import java.io.BufferedReader;
import java.io.IOException;

import japsa.seq.SequenceReader;
import japsa.util.CommandLine;
import japsa.util.IntArray;
import japsa.util.deploy.Deployable;

/**
 * @author minhduc
 *
 */
@Deprecated
@Deployable(
	scriptName = "jsa.np.mlstStrainTyping", 
	scriptDesc = "Strain typing using MLST system"
	)
public class MLSTStrainTypingCmd extends CommandLine{	
	public MLSTStrainTypingCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		addString("mlst", "MLST_profiles.txt",  "MLST file");
		addString("bamFile", null,  "The bam file");
		addString("geneFile", null,  "The gene file");		

		addInt("top", 10,  "The number of top strains");		
		addString("msa", "kalign",
				"Name of the msa method, support poa, kalign, muscle and clustalo");
		addString("tmp", "tmp/t",  "Temporary folder");
		addString("hours", null,  "The file containging hours against yields, if set will output acording to tiime");

		addInt("read", 500,  "Number of reads before a typing, NA if timestamp is set");
		addBoolean("twodonly", false,  "Use only two dimentional reads");		


		addStdHelp();		
	} 

	public static void main(String[] args) throws IOException, InterruptedException{
		CommandLine cmdLine = new MLSTStrainTypingCmd();
		args = cmdLine.stdParseLine(args);			
		/**********************************************************************/

		String mlst = cmdLine.getStringVal("mlst");
		String bamFile = cmdLine.getStringVal("bamFile");
		String geneFile = cmdLine.getStringVal("geneFile");
		String msa = cmdLine.getStringVal("msa");
		String tmp = cmdLine.getStringVal("tmp");		
		String hours = cmdLine.getStringVal("hours");

		int top = cmdLine.getIntVal("top");		
		int read = cmdLine.getIntVal("read");		

		boolean twodonly = cmdLine.getBooleanVal("twodonly");


		MLSTStrainTyping paTyping = new MLSTStrainTyping();		
		paTyping.msa = msa;
		paTyping.prefix = tmp;			

		paTyping.twoDOnly = twodonly;
		paTyping.readNumber = read;
		if (hours !=null){
			BufferedReader bf = SequenceReader.openFile(hours);
			String line = bf.readLine();//first line
			paTyping.hoursArray = new IntArray();
			paTyping.readCountArray = new IntArray();

			while ((line = bf.readLine())!= null){
				String [] tokens = line.split("\\s");
				int hrs = Integer.parseInt(tokens[0]);
				int readCount = Integer.parseInt(tokens[2]);

				paTyping.hoursArray.add(hrs);
				paTyping.readCountArray.add(readCount);	
			}
		}


		if (paTyping.readNumber < 1)
			paTyping.readNumber = 1;

		paTyping.readGenes(geneFile);
		paTyping.readMLSTProfiles(mlst);
		paTyping.typing(bamFile,  top);	

	}
}
