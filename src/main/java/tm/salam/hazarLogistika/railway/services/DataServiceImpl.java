package tm.salam.hazarLogistika.railway.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tm.salam.hazarLogistika.railway.dtos.*;
import tm.salam.hazarLogistika.railway.helper.FileUploadUtil;
import tm.salam.hazarLogistika.railway.helper.ResponseTransfer;
import tm.salam.hazarLogistika.railway.models.Data;
import tm.salam.hazarLogistika.railway.daos.DataRepository;
import tm.salam.hazarLogistika.railway.models.ExcelFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DataServiceImpl implements DataService{

    private final DataRepository dataRepository;
    private final ExcelReaderService excelService;
    private final ExcelFileService excelFileService;
    private final StationService stationService;
    private final StatusVanService statusVanService;
    private final TypeVanService typeVanService;
    private final VanService vanService;
    private final DocumentService documentService;

    @Autowired
    public DataServiceImpl(DataRepository dataRepository, ExcelReaderService excelService,
                           ExcelFileService excelFileService, StationService stationService,
                           StatusVanService statusVanService, TypeVanService typeVanService,
                           VanService vanService, DocumentService documentService) {
        this.dataRepository = dataRepository;
        this.excelService = excelService;
        this.excelFileService=excelFileService;
        this.stationService = stationService;
        this.statusVanService = statusVanService;
        this.typeVanService = typeVanService;
        this.vanService = vanService;
        this.documentService = documentService;
    }

    @Override
    @Transactional
    public ResponseTransfer loadDataInExcelFile(final MultipartFile excelFile) throws InterruptedException {

        String fileName= StringUtils.cleanPath(excelFile.getOriginalFilename());
        String extension="";
        for(int i=fileName.length()-1;i>=0;i--){

            extension=fileName.charAt(i)+extension;
            if(fileName.charAt(i)=='.'){
                break;
            }
        }
        Thread.sleep(10);
        fileName="hazar_logistika "+new Timestamp(new Date().getTime())+extension;
        final ExcelFileDTO checkFile=excelFileService.getExcelFileDTOByName(fileName.toString());

        if(checkFile!=null){

            return new ResponseTransfer("excel file already added with such name",false);
        }
        final String uploadDir="src/main/resources/excelFiles/data/";
        final ExcelFileDTO excelFileDTO=ExcelFileDTO.builder()
                .name(fileName)
                .path(uploadDir)
                .build();

        List<HashMap<Integer, List<Object>>>data=null;


        try {

            FileUploadUtil.saveFile(uploadDir,fileName,excelFile);
            if(!excelFileService.saveExcelFile(excelFileDTO).getStatus().booleanValue()){

                return new ResponseTransfer("parameter excel file don't added",false);
            }
        } catch (IOException e) {
            e.printStackTrace();

            return new ResponseTransfer("error with saving excel file",false);
        }

        documentService.saveDocument(fileName);

        try {

            data=excelService.read(uploadDir+fileName);

        } catch (IOException e) {
            e.printStackTrace();

            return new ResponseTransfer("error with reading excel file",false);
        }


        ExcelFile savedExcelFile =excelFileService.getExcelFileByName(excelFileDTO.getName());

        for(HashMap<Integer,List<Object>>helper:data){

            Map<Integer,String>indexValues=new HashMap<>();

            for(Integer key:helper.keySet()){

                List<Object>dataList=helper.get(key);
                DataDTO dataDTO=null;

                for(int i=0;i<dataList.size();i++){

                    switch (dataList.get(i).toString()){

                        case "№вагона":
                            indexValues.put(i,"numberVan");
                            break;
                        case "Код \n" +
                                "собст.":
                            indexValues.put(i,"codeOfTheProperty");
                            break;
                        case "Станция":
                            indexValues.put(i,"currentStation");
                            break;
                        case "Код":
                            indexValues.put(i,"statusVan");
                            break;
                        case "Год":
                            indexValues.put(i,"year");
                            break;
                        case "Дата":
                            indexValues.put(i,"date");
                            break;
                        case "Время":
                            indexValues.put(i,"time");
                            break;
                        case "Состояние":
                            indexValues.put(i,"typeVan");
                            break;
                        case "Станция \n" +
                                "назн.вагона":
                            indexValues.put(i,"setStation");
                            break;
                        case "Индекс поезда":
                            indexValues.put(i,"indexTrain");
                            break;
                        default:
                            if(indexValues.containsKey(i)){

                                if(Objects.equals(indexValues.get(i),"numberVan")){
                                    if(dataList.get(i)==" " || dataList.get(i)==null){
                                        break;
                                    }
                                }else if(dataDTO==null || dataDTO.getNumberVan()==null){
                                    break;
                                }
                                dataDTO=setValueDataDTO(indexValues.get(i),dataList.get(i),dataDTO);
                            }else{
                                break;
                            }
                    }
                }
                if(dataDTO!=null && dataDTO.getNumberVan()!=null && dataDTO.getNumberVan()!=" "){

                    Data temporal=dataRepository.getLastDataByNumberVan(dataDTO.getNumberVan());

                    if((dataDTO.getYear()!=null && dataDTO.getDate()!=null) || dataDTO.getTime()!=null){
                        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd.MM.yyyy HH-mm");

                        if(dataDTO.getTime()==null){

                            dataDTO.setTime("00-00");
                        }
                        dataDTO.setYear(2000+dataDTO.getYear());
                        try {

                            String s=dataDTO.getDate()+"."+dataDTO.getYear().intValue()+" "+dataDTO.getTime();
                            dataDTO.setYearDateTime(simpleDateFormat.parse(dataDTO.getDate() + "." +
                                                                dataDTO.getYear().intValue()+" " + dataDTO.getTime()));

                        }catch (ParseException exception){

                            exception.printStackTrace();
                        }

                    }
                    if(temporal==null){

                        dataDTO.setLastStation(dataDTO.getCurrentStation());
                        dataDTO.setHourForPassedWay(0L);
                    }else{

                        dataDTO.setLastStation(temporal.getCurrentStation());
                        if(dataDTO.getYearDateTime()!=null){

                            long diff=dataDTO.getYearDateTime().getTime()-temporal.getYearDateTime().getTime();
                            dataDTO.setHourForPassedWay(TimeUnit.MILLISECONDS.toHours(diff));
                        }
                    }
                    VanDTO vanDTO=vanService.getVanDTOByCode(dataDTO.getNumberVan());

                    if(vanDTO==null || vanDTO.getDateNextRepear()==null){

                        dataDTO.setDayForRepair(null);
                    }else{
                        long diff=vanDTO.getDateNextRepear().getTime()-(new Date()).getTime();
                        dataDTO.setDayForRepair(TimeUnit.MILLISECONDS.toDays(diff));
                    }
                    Boolean act=false;

                    if(vanDTO!=null && vanDTO.getDateAct()!=null){

                        act=true;
                    }
                    Data saveData=Data.builder()
                            .numberVan(dataDTO.getNumberVan())
                            .codeOfTheProperty(dataDTO.getCodeOfTheProperty())
                            .lastStation(dataDTO.getLastStation())
                            .currentStation(dataDTO.getCurrentStation())
                            .statusVan(dataDTO.getStatusVan())
                            .year(dataDTO.getYear())
                            .date(dataDTO.getDate())
                            .time(dataDTO.getTime())
                            .yearDateTime(dataDTO.getYearDateTime())
                            .typeVan(dataDTO.getTypeVan())
                            .setStation(dataDTO.getSetStation())
                            .act(act)
                            .hourForPassedWay(dataDTO.getHourForPassedWay())
                            .dayForRepair(dataDTO.getDayForRepair())
                            .indexTrain(dataDTO.getIndexTrain())
                            .excelFile(savedExcelFile)
                            .build();

                    dataRepository.save(saveData);
                }
            }
        }

        return new ResponseTransfer("data successful saved",true);
    }

    private DataDTO setValueDataDTO(final String variableName, final Object value, DataDTO dataDTO) {

        if(dataDTO==null){

            dataDTO=new DataDTO();
        }
        switch (variableName){

            case "numberVan":
                dataDTO.setNumberVan(value.toString());
                break;
            case "codeOfTheProperty":
                dataDTO.setCodeOfTheProperty(value.toString());
                break;
            case "currentStation":
                dataDTO.setCurrentStation(getFullNameStation(value.toString()));
                break;
            case "statusVan":
                dataDTO.setStatusVan(getStatusVanFullName(value.toString()));
                break;
            case "year":
                if(valueIsNumericType(value.toString())){

                    dataDTO.setYear(Double.parseDouble(value.toString()));
                }
                break;
            case "date":
                dataDTO.setDate(value.toString());
                break;
            case "time":
                dataDTO.setTime(value.toString());
                break;
            case "typeVan":
                dataDTO.setTypeVan(getFullNameTypeVan(value.toString()));
                break;
            case "setStation":
                dataDTO.setSetStation(getFullNameStation(value.toString()));
                break;
            case "indexTrain":
                dataDTO.setIndexTrain(value.toString());
                break;
        }

        return dataDTO;
    }

    private boolean valueIsNumericType(String value) {

        try {

            Double.parseDouble(value.toString());

            return true;
        }catch (NumberFormatException exception){

            return false;
        }
    }

    private String getStatusVanFullName(final String shortName) {

        StatusVanDTO statusVanDTO=statusVanService.getStatusVanDTOByShortName(shortName);

        if(statusVanDTO==null){

            return null;
        }else{

            return statusVanDTO.getFullName();
        }
    }

    private String getFullNameTypeVan(final String shortName) {

        TypeVanDTO typeVanDTO=typeVanService.getTypeVanDTOByShortName(shortName);

        if(typeVanDTO==null){

            return null;
        }else{

            return typeVanDTO.getFullName();
        }
    }

    private String getFullNameStation(final String shortName) {

        StationDTO stationDTO=stationService.getStationDTOBYShortName(shortName);

        if(stationDTO==null){

            return shortName;
        }else{

            return stationDTO.getFullName();
        }
    }

    @Override
    public List<OutputDataDTO> getAllData(List<Integer> idExcelFiles, List<String> currentStation, List<String> setStation,
                                          List<String> typeVan, List<Boolean>actAcceptense, Date initialDate, Date finalDate,
                                          String numberVan){

        if(idExcelFiles==null || idExcelFiles.isEmpty()){

            idExcelFiles=excelFileService.getNameAllExcelFiles();
        }
        if(currentStation==null || currentStation.isEmpty()){

            currentStation=dataRepository.getCurrentStationsFromData(idExcelFiles).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }else{

            currentStation=currentStation.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        if(setStation==null || setStation.isEmpty()){

            setStation=dataRepository.getSetStationsFromData(idExcelFiles).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }else{

            setStation=setStation.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        if(typeVan==null || typeVan.isEmpty()){

            typeVan=typeVanService.getAllFullNameTypeVan().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }else{

            typeVan=typeVan.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        if(actAcceptense==null || actAcceptense.isEmpty()){

            actAcceptense=new ArrayList<>(
                    List.of(true,false)
            );
        }
        List<Data>data=null;

        if(numberVan==null){
            numberVan="";
        }
        numberVan='%'+numberVan+'%';
        if(initialDate == null && finalDate == null){

            data=dataRepository.getAllDataByExcelFileIdsAndCurrentStationsAndSetStationsAndTypeVans(idExcelFiles,
                                                                                                    currentStation,
                                                                                                    setStation,
                                                                                                    typeVan,
                                                                                                    actAcceptense,
                                                                                                    numberVan);
        }else{

            data=dataRepository.
                    getAllDataByExcelFileIdsAndCurrentStationsAndSetStationsAndTypeVansAndBetweenDates(idExcelFiles,
                                                                                                        currentStation,
                                                                                                        setStation,
                                                                                                        typeVan,
                                                                                                        actAcceptense,
                                                                                                        initialDate,
                                                                                                        finalDate,
                                                                                                        numberVan);
        }
        List<OutputDataDTO>outputDataDTOList=new ArrayList<>();

        for(Data helper:data){

            if(helper.getDayForRepair()==null){

                VanDTO vanDTO=vanService.getVanDTOByCode(helper.getNumberVan());

                if(vanDTO!=null && vanDTO.getDateNextRepear()!=null){

                    long diff=vanDTO.getDateNextRepear().getTime()-(new Date()).getTime();

                    helper.setDayForRepair(TimeUnit.MILLISECONDS.toDays(diff));
                    dataRepository.save(helper);
                }
            }

            outputDataDTOList.add(
                    OutputDataDTO.builder()
                            .id(helper.getId())
                            .numberVan(helper.getNumberVan())
                            .lastStation(helper.getLastStation())
                            .currentStation(helper.getCurrentStation())
                            .statusVan(helper.getStatusVan())
                            .date(helper.getYearDateTime())
                            .typeVan(helper.getTypeVan())
                            .setStation(helper.getSetStation())
                            .act(helper.getAct())
                            .hourForPassedWay(helper.getHourForPassedWay())
                            .dayForRepair(helper.getDayForRepair())
                            .build()
            );
        }

        return outputDataDTOList;
    }

    @Override
    public List<String> getCurrentStationsFromData(List<Integer>idExcelFiles){

        if(idExcelFiles==null || idExcelFiles.isEmpty()){

            List<Integer>helper=new ArrayList<>();

            excelFileService.getAllExcelFileDTO().forEach(excelFileDTO ->
            {
                helper.add(excelFileDTO.getId());
            });
            return dataRepository.getCurrentStationsFromData(helper);
        }else {
            return dataRepository.getCurrentStationsFromData(idExcelFiles);
        }
    }

    @Override
    public List<String>getSetStationsFromData(List<Integer>idExcelFiles){

        if(idExcelFiles==null || idExcelFiles.isEmpty()){

            List<Integer>helper=new ArrayList<>();

            excelFileService.getAllExcelFileDTO().forEach(excelFileDTO ->
            {
                helper.add(excelFileDTO.getId());
            });

            return dataRepository.getSetStationsFromData(helper);
        }else {

            return dataRepository.getSetStationsFromData(idExcelFiles);
        }
    }

    @Override
    public File getExcelFileById(final int id){

        ExcelFileDTO excelFileDTO=excelFileService.getExcelFileDTOById(id);
        File file=new File(excelFileDTO.getPath()+excelFileDTO.getName());

        if(file.exists()) {

            documentService.exportDocument(excelFileDTO.getName());

            return file;
        }else{

            return null;
        }
    }

}