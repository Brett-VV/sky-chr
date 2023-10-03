package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetMealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.SetMeal;
import com.sky.entity.SetMealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetMealServiceImpl implements SetMealService {

    @Autowired
    private SetMealMapper setMealMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        SetMeal setMeal = new SetMeal();

        BeanUtils.copyProperties(setmealDTO, setMeal);

        //向套餐表插入数据
        setMealMapper.insert(setMeal);

        //获取生成的套餐id
        Long setMealId = setMeal.getId();

        List<SetMealDish> setMealDishes = setmealDTO.getSetmealDishes();

        setMealDishes.forEach(setMealDish -> {
            setMealDish.setSetmealId(setMealId);
        });

        //保存套餐和菜品的关联关系
        setMealDishMapper.insertBatch(setMealDishes);
    }

    /**
     * 分页查询
     *
     * @param setMealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetMealPageQueryDTO setMealPageQueryDTO) {
        int pageNum = setMealPageQueryDTO.getPage();
        int pageSize = setMealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<SetmealVO> page = setMealMapper.pageQuery(setMealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            SetMeal setMeal = setMealMapper.getById(id);
            if (StatusConstant.ENABLE == setMeal.getStatus()) {
                //起售套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(setMealId ->{
            //删除套餐表的数据
            setMealMapper.deleteById(setMealId);
            //删除套餐菜品表数据
            setMealDishMapper.deleteBySetMealId(setMealId);
        });
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        SetMeal setMeal = setMealMapper.getById(id);
        List<SetMealDish> setMealDishes = setMealDishMapper.getBySetMealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setMeal,setmealVO);
        setmealVO.setSetmealDishes(setMealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        SetMeal setMeal = new SetMeal();
        BeanUtils.copyProperties(setmealDTO,setMeal);

        //1.修改套餐，执行update
        setMealMapper.update(setMeal);

        //获取套餐id
        Long setMealId = setmealDTO.getId();

        //2.删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setMealDishMapper.deleteBySetMealId(setMealId);

        List<SetMealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setMealDish -> {
            setMealDish.setSetmealId(setMealId);
        });

        //3.重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        setMealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status == StatusConstant.ENABLE){
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if (dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if (StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        SetMeal setMeal = SetMeal.builder()
                .id(id)
                .status(status)
                .build();
        setMealMapper.update(setMeal);
    }
}
