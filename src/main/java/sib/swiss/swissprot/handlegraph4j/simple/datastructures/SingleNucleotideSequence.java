/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SingleNucleotideSequence implements Sequence {

    private final byte nucleotide;

    SingleNucleotideSequence(byte nucleotide) {
        this.nucleotide = nucleotide;
    }

    @Override
    public byte byteAt(int offset) {
        if (offset == 0) {
            return nucleotide;
        } else {
            throw new IndexOutOfBoundsException("Single NP can only have offest 0");
        }
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public SequenceType getType() {
        return SequenceType.OTHER;
    }

    @Override
    public Sequence reverseCompliment() {
        return new SingleNucleotideSequence(Sequence.compliment(nucleotide));
    }

}
