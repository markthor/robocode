/*
 * Copyright (C) 2004 Derek James and Philip Tucker
 * 
 * This file is part of ANJI (Another NEAT Java Implementation).
 * 
 * ANJI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * 
 * created by Philip Tucker
 */
package com.anji.integration;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.jgap.Allele;
import org.jgap.Chromosome;
import org.jgap.event.GeneticEvent;
import org.jgap.event.GeneticEventListener;

import com.anji.neat.ConnectionAllele;
import com.anji.neat.NeuronAllele;
import com.anji.neat.NeuronType;
import com.anji.run.Run;
import com.anji.util.Properties;

/**
 * Stores presentation XML data where it can be displayed, likely in a web browser.
 * 
 * @author Philip Tucker
 */
public class PresentationEventListener implements GeneticEventListener {

/**
 * poperties key, directory in which to store presentation data
 */
public final static String BASE_DIR_KEY = "presentation.dir";

private final static String FITNESS_DIR = "fitness/";

private final static String FITNESS_FILE = "fitness.xml";

private final static String SPECIES_DIR = "species/";

private final static String SPECIES_FILE = "species.xml";

private final static String COMPLEXITY_DIR = "complexity/";

private final static String COMPLEXITY_FILE = "complexity.xml";

private final static String DATA_DIR = "data/";

private final static String DATA_FILE = "run.dat";

private static Logger logger = Logger.getLogger( PresentationEventListener.class );

private File fitnessDir;

private File speciesDir;

private File complexityDir;

private File dataDir;

private Run run;

private static File mkdir( String path ) {
	File result = new File( path );
	result.mkdirs();
	if ( !result.exists() )
		throw new IllegalArgumentException( "base directory does not exist: " + path );
	if ( !result.isDirectory() )
		throw new IllegalArgumentException( "base directory is a file: " + path );
	if ( !result.canWrite() )
		throw new IllegalArgumentException( "base directory not writable: " + path );
	return result;
}


/**
 * ctor
 * @param aRun
 */
public PresentationEventListener( Run aRun ) {
	run = aRun;
}

/**
 * See <a href=" {@docRoot}/params.htm" target="anji_params">Parameter Details </a> for
 * specific property settings.
 * 
 * @param props configuration data
 */
public void init( Properties props ) {
	String basePath = props.getProperty( BASE_DIR_KEY );
	fitnessDir = mkdir( basePath + File.separator + FITNESS_DIR );
	speciesDir = mkdir( basePath + File.separator + SPECIES_DIR );
	complexityDir = mkdir( basePath + File.separator + COMPLEXITY_DIR );
	dataDir = mkdir( basePath + File.separator + DATA_DIR );
}

/**
 * @param event <code>GeneticEvent.GENOTYPE_EVALUATED_EVENT</code> is the only event handled;
 * writes presentation data for run
 */
public void geneticEventFired( GeneticEvent event ) {
	if ( GeneticEvent.GENOTYPE_EVALUATED_EVENT.equals( event.getEventName() ) ) {
		storeRun( false );
	}
	else if ( GeneticEvent.RUN_COMPLETED_EVENT.equals( event.getEventName() ) ) {
		storeRun( true );
	}
}

/**
 * Store/update run presentation data based on <code>genotype</code>.
 * @param isRunCompleted <code>true</code> iff this is the last call to
 * <code>storeRun()</code> for this run
 */
public void storeRun( boolean isRunCompleted ) {
	FileWriter fitnessOut = null;
	FileWriter speciesOut = null;
	FileWriter complexityOut = null;
	FileWriter dataOut = null;
	try {
		fitnessOut = new FileWriter( fitnessDir.getAbsolutePath() + File.separator + FITNESS_FILE );
		speciesOut = new FileWriter( speciesDir.getAbsolutePath() + File.separator + SPECIES_FILE );
		complexityOut = new FileWriter( complexityDir.getAbsolutePath() + File.separator + COMPLEXITY_FILE );
		if(isFirstGeneration()) {
			dataOut = new FileWriter( dataDir.getAbsolutePath() + File.separator + DATA_FILE, false );
			dataOut.write( buildDataFileHeaders() );
		} else {
			dataOut = new FileWriter( dataDir.getAbsolutePath() + File.separator + DATA_FILE, true );
			dataOut.write( buildDataFileContent() );
		}
		

		XmlPersistableRun xmlRun = new XmlPersistableRun( run );
		complexityOut.write( xmlRun.toComplexityString( isRunCompleted ) );
		fitnessOut.write( xmlRun.toFitnessString( isRunCompleted ) );
		speciesOut.write( xmlRun.toSpeciesString( isRunCompleted ) );
	}
	catch ( Throwable e ) {
		logger.error( "PresentationEventListener: error storing run", e );
	}
	finally {
		try {
			if ( complexityOut != null )
				complexityOut.close();
			if ( speciesOut != null )
				speciesOut.close();
			if ( fitnessOut != null )
				fitnessOut.close();
			if( dataOut != null) {
				dataOut.close();
			}
		}
		catch ( Exception e ) {
			logger.error( "error closing presentation files" );
		}
	}
}

@SuppressWarnings("unchecked")
public String buildDataFileContent() {
	StringBuilder dataBuilder = new StringBuilder();
	
	Generation curGen = getCurrentGeneration();
	if(curGen != null) {
		Chromosome fittestChromosome = curGen.getGenoType().getFittestChromosome();
		
		if(fittestChromosome != null) {
			
			SortedSet<Allele> alleles = fittestChromosome.getAlleles();
			Set<Long> inputNeurons = new HashSet<Long>();
			Set<Long> neurons = new HashSet<Long>();
			for(Allele allele : alleles) {
				if ( allele instanceof NeuronAllele ) {
					NeuronAllele nAllele = (NeuronAllele) allele;
					if(nAllele.getType() == NeuronType.INPUT) {
						inputNeurons.add(nAllele.getId());
					}
					neurons.add(nAllele.getId());
				}
			}
			int connectedInputNeurons = 0;
			int connections = 0;
			Set<Long> connectedNeurons = new HashSet<Long>();
			for(Allele allele : alleles) {
				if ( allele instanceof ConnectionAllele ) {
					connections++;
					ConnectionAllele cAllele = (ConnectionAllele) allele;
					Long srcNeuronId = cAllele.getSrcNeuronId();
					Long destNeuronId = cAllele.getDestNeuronId();
					if(inputNeurons.contains(srcNeuronId)) {
						connectedInputNeurons++;
					}
					connectedNeurons.add(srcNeuronId);
					connectedNeurons.add(destNeuronId);
					
				}
			}
			int maxScore = fittestChromosome.getFitnessValue();
			int networkSize = connectedNeurons.size() + connections;
			
			dataBuilder.append(getCurrentGenerationNumber() + "\t");
			dataBuilder.append(maxScore + "\t");
			dataBuilder.append(connectedInputNeurons + "\t");
			dataBuilder.append(connections + "\t");
			dataBuilder.append(connectedNeurons.size() + "\t");
			dataBuilder.append(networkSize + "\n");
		}
	}
	return dataBuilder.toString();
}

private String buildDataFileHeaders() {
	StringBuilder dataBuilder = new StringBuilder();
	dataBuilder.append("generation\t");
	dataBuilder.append("max-score\t");
	dataBuilder.append("connected-input-neurons\t");
	dataBuilder.append("connections\t");
	dataBuilder.append("connected-neurons\t");
	dataBuilder.append("network-size\n");
	return dataBuilder.toString();
}

private boolean isFirstGeneration() {
	return getCurrentGenerations().size() == 1;
}

private Generation getCurrentGeneration() {
	List<Generation> generations = getCurrentGenerations();
	int generation = generations.size();
	if(0 < generation) {
		return generations.get(generation-1);
	}
	return null;
}

private List<Generation> getCurrentGenerations() {
	return (List<Generation>) run.getGenerations();
}

private int getCurrentGenerationNumber() {
	return getCurrentGenerations().size();
}

}
