/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

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
    public Sequence reverseComplement() {
        return new SingleNucleotideSequence(Sequence.complement(nucleotide));
    }

    @Override
    public int hashCode() {
        return Sequence.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof SingleNucleotideSequence) {
            SingleNucleotideSequence other = (SingleNucleotideSequence) obj;
            return this.nucleotide == other.nucleotide;
        } else if (obj instanceof Sequence) {
            return Sequence.equalByBytes(this, (Sequence) obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return Character.toString(nucleotide);
    }
    
    
}
