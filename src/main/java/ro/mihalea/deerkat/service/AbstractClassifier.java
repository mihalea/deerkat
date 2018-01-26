package ro.mihalea.deerkat.service;

import java.util.List;

public abstract class AbstractClassifier<DataType> {
    protected List<DataType> modelData;
//    protected List<CategoryType>

    public void addModelList(List<DataType> data) {
        modelData.addAll(data);
    }

    public void addModelItem(DataType data) {
        modelData.add(data);
    }
}
