/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import io.github.jervenbolleman.handlegraph4j.sequences.SequenceType;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
