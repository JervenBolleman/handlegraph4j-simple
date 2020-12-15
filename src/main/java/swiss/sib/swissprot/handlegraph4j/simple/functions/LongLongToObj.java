/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.functions;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
@FunctionalInterface
public interface LongLongToObj<T> {

    public T apply(long key, long value);
    
}
