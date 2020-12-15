package swiss.sib.swissprot.handlegraph4j.simple.functions;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
@FunctionalInterface
public interface ToLong<T> {

    public long apply(T t);
    
}
