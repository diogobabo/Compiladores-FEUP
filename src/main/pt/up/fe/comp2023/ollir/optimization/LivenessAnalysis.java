package pt.up.fe.comp2023.ollir.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class LivenessAnalysis {
    private final HashMap<Integer,HashSet<String>> def;

    private final HashMap<Integer,HashSet<String>> use;

    private final HashMap<Integer,HashSet<String>> in;

    private final HashMap<Integer,HashSet<String>> out;

    private final Method method;

    private final List<Instruction> instructionList;

    public LivenessAnalysis(Method method) {
        this.def = new HashMap<>();
        this.use = new HashMap<>();
        this.in = new HashMap<>();
        this.out = new HashMap<>();
        this.method = method;
        this.instructionList = method.getInstructions();
        this.initializeMaps(method.getInstructions().size());
    }

    public void analyse() {
        for(Instruction inst: instructionList) {
            getWrittenVars(inst);
            getReadVars(inst);
        }

        int numInst = instructionList.size();
        boolean changes;
        do {
            changes = false;
            for (int i = numInst - 1; i >= 0; i--) {
                Instruction inst = instructionList.get(i);
                int instId = inst.getId();
                HashSet<String> tempIn = this.in.get(inst.getId());
                HashSet<String> tempOut = this.out.get(inst.getId());

                this.out.put(instId,calcOut(inst));
                this.in.put(instId,calcIn(inst));

                changes = !(tempIn.equals(this.in.get(inst.getId())) && tempOut.equals(this.out.get(inst.getId())));
            }
        } while(changes);
        deadCodeFix(numInst);
        return;
    }

    public HashSet<String> calcIn(Instruction inst) {
        int instId = inst.getId();
        HashSet<String> res = new HashSet<>(this.use.get(instId));
        HashSet<String> tmpOut = new HashSet<>(this.out.get(instId));

        for(String x: this.def.get(instId)) {
            tmpOut.remove(x);
        }

        res.addAll(tmpOut);
        return res;
    }

    public void deadCodeFix(int numInst) {
        for(int i = 1; i <= numInst; i++) {
            HashSet<String> def = new HashSet<>(this.def.get(i));
            HashSet<String> out = new HashSet<>(this.out.get(i));
            out.addAll(def);
            this.out.put(numInst,out);
        }
    }

    public HashSet<String> calcOut(Instruction inst) {
        HashSet<String> res = new HashSet<>();
        for (Node succ : inst.getSuccessors()) {
            if(this.in.get(succ.getId()) != null) {
                HashSet<String> tmp = new HashSet<>(this.in.get(succ.getId()));
                res.addAll(tmp);
            }
        }
        return res;
    }
    public void parseDefUse() {
        for(Instruction inst: instructionList) {
            getWrittenVars(inst);
            getReadVars(inst);
        }
    }

    public void initializeMaps(int numInst) {
        for(int i = 1; i <= numInst; i++) {
            def.put(i,new HashSet<>());
            use.put(i,new HashSet<>());
            in.put(i,new HashSet<>());
            out.put(i,new HashSet<>());
        }
    }

    public void getWrittenVars(Instruction inst) {
        if(Objects.equals(inst.getInstType().name(),"ASSIGN")) {
            AssignInstruction assign = (AssignInstruction) inst;
            String definedVar = ((Operand) assign.getDest()).getName();
            int instId = inst.getId();
            addVarToSet(this.def,instId,definedVar);
        }
    }

    public void addVarToSet(HashMap<Integer, HashSet<String>> map, Integer id, String var) {
        HashSet<String> tempDef = map.get(id);
        if(tempDef != null) {
            tempDef.add(var);
            map.put(id,tempDef);
        }
        else {
            HashSet<String> tempiDef = new HashSet<>();
            tempiDef.add(var);
            map.put(id,tempiDef);
        }
    }



    public void getReadVars(Instruction inst) {
        switch(inst.getInstType().name()) {
            case "ASSIGN" -> getReadVars(((AssignInstruction) inst).getRhs());
            case "RETURN" -> getReturnVars((ReturnInstruction) inst);
            case "BINARYOPER" -> getBinaryOpVars((BinaryOpInstruction) inst);
            case "UNARYOPER" -> getUnaryOpVar((UnaryOpInstruction) inst);
            case "PUTFIELD" -> getPutField((PutFieldInstruction) inst);
            case "GETFIELD" -> getField((GetFieldInstruction) inst);
            case "NOPER" -> getNoper((SingleOpInstruction) inst);
            case "CALL" -> getMethodCall((CallInstruction) inst);
            case "BRANCH" -> getBranch((CondBranchInstruction) inst);
        };
    }

    public void getReturnVars(ReturnInstruction inst) {
        String varName = getVarName(inst.getOperand());
        if(varName != null) {
            this.addVarToSet(this.use,inst.getId(),varName);
        }
    }

    public void getNoper(SingleOpInstruction inst) {
        Element firstOp = inst.getSingleOperand();
        if(!firstOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(firstOp));
        }
    }

    public void getBranch(CondBranchInstruction inst) {
        SingleOpCondInstruction sInst = (SingleOpCondInstruction) inst;
        getReadVars(sInst.getCondition());
    }

    public void getMethodCall(CallInstruction inst) {
        int a;
        List<Element> params = inst.getListOfOperands();
        for(Element param: params) {
            if(!param.isLiteral()) {
                this.addVarToSet(this.use,inst.getId(),getVarName(param));
            }
        }
    }

    public void getBinaryOpVars(BinaryOpInstruction inst) {
        Element leftOp = inst.getLeftOperand();
        Element rightOp = inst.getRightOperand();

        if(!leftOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(leftOp));
        }
        if(!rightOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(rightOp));
        }
    }

    public void getUnaryOpVar(UnaryOpInstruction inst) {
        Element rightOp = inst.getOperand();
        if(!rightOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(rightOp));
        }
    }

    public void getField(GetFieldInstruction inst) {
        Element secondOp = inst.getSecondOperand();
        if(!secondOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(secondOp));
        }
    }

    public void getPutField(PutFieldInstruction inst) {
        Element secondOp = inst.getSecondOperand();
        Element thirdOp = inst.getThirdOperand();

        if(!secondOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(secondOp));
        }
        if(!thirdOp.isLiteral()) {
            this.addVarToSet(this.use,inst.getId(),getVarName(thirdOp));
        }
    }

    public String getVarName(Element op) {
        return !op.isLiteral() ? ((Operand) op).getName() : null;
    }

}
