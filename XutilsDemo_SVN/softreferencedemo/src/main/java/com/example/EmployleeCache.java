package com.example;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Hashtable;

public class EmployleeCache {
    static private EmployleeCache employeeCache;
    private Hashtable<String,EmployeeRef> employeeRefs;
    private ReferenceQueue<Employee> q;//垃圾回收器的对列

    private class EmployeeRef extends SoftReference<Employee> {
        private String _key="";

        public EmployeeRef(Employee em,ReferenceQueue<Employee> q)
        {
            super(em,q);
            _key=em.getId();
        }
    }


    private EmployleeCache(){
        employeeRefs=new Hashtable<String,EmployeeRef>();
        q=new ReferenceQueue<Employee>();
    }

    public static synchronized EmployleeCache getInstance(){
        if(employeeCache==null){
            employeeCache=new EmployleeCache();
        }
        return employeeCache;
    }

    /**
     * 以软引用的方式对一个Employee对象的实例进行引用并保存该引用
     * @param em
     */
    private void cacheEmployee(Employee em){
        cleanCache();//清除垃圾引用
        EmployeeRef ref=new EmployeeRef(em,q);
        employeeRefs.put(em.getId(),ref);
    }

    /*
     * 根据指定的id号,重新获取相应的Employee对象的实例
     * @param id
     * @return
     */
    public Employee getEmployee(String id){

        Employee em=null;
        //进行判断缓存中的是否有该Employee实例的软引用,如果有,从软引用中取得
        if(employeeRefs.containsKey(id)){
            EmployeeRef ref=(EmployeeRef)(employeeRefs.get(id));
            em=(Employee)ref.get();
        }

        //如果没有软引用,或者从本软引用中得到的实例为Null,重新构建一个实例
        if(em==null){
            em=new Employee(id);
            this.cacheEmployee(em);
        }

        return em;
    }

    /**
     * 清除那些软引用的Employee对象
     * 已经被回收的EmployeeRef的对象
     */
    private void cleanCache(){
        //进行定义引用
        EmployeeRef ref=null;
        while((ref=(EmployeeRef)q.poll())!=null){
            employeeRefs.remove(ref._key);
        }
    }
}
