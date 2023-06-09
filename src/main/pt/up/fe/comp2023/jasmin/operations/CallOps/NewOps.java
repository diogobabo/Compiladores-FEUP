package pt.up.fe.comp2023.jasmin.operations.CallOps;

import org.specs.comp.ollir.*;
import pt.up.fe.comp2023.jasmin.Jasmin;

import java.util.HashMap;

public class NewOps extends InvokeAbstract{
    public NewOps(CallInstruction instruction, HashMap<String, Descriptor> VarTable,
                  int LabelCounter, String ThisClassName, HashMap<String, String> importsMap, Jasmin jasmin) {
        super(instruction, VarTable, LabelCounter, ThisClassName, importsMap, jasmin);
    }

    @Override
    public String toJasmin() {
        StringBuilder jasminCode = new StringBuilder();
        CallInstruction instruction = (CallInstruction) this.getInstruction();

        ElementType elementType = instruction.getReturnType().getTypeOfElement();

        if (elementType == ElementType.OBJECTREF) {
            for (Element element : instruction.getListOfOperands()) {
                jasminCode.append(super.loadElement(element));
            }
            jasminCode.append("\tnew ").append(this.importsMap.getOrDefault
                    (((Operand) instruction.getFirstArg()).getName(),((Operand) instruction.getFirstArg()).getName())).append("\n");
            super.jasmin.growStackSize(1);
            return jasminCode.toString();

        } else if (elementType == ElementType.ARRAYREF) {
            for (Element element : instruction.getListOfOperands()) {
                jasminCode.append(super.loadElement(element));
            }
            jasminCode.append("\tnewarray ");
            if (instruction.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32) {
                jasminCode.append("int\n");
            }
        }
        jasminCode.append("\tdup\n");
        super.jasmin.growStackSize(1);
        for (int a = 0; a< instruction.getListOfOperands().size()-1 ; a++) {
            super.jasmin.lowerStackSize();
        }
        return jasminCode.toString();
    }
}
